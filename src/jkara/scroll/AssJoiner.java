package jkara.scroll;

import ass.model.AssStyle;
import ass.model.AssStyleKey;
import ass.model.DialogLine;
import ass.model.ParsedAss;
import ass.parser.AssParser;
import jkara.util.Util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntPredicate;

public final class AssJoiner {

    // todo: нужно сгруппировать AssLines по принципу "расстояние по времени между соседними строками < 1 сек"
    // todo: потом эти группы побить на группы с фиксированным кол-вом строк (например 4)
    // todo: показываем так:
    //   *1  1     5 *5  5 ...
    //    2 *2     6  6 *6
    //       3 *3  3     7
    //       4  4 *4     8

    private static List<List<DialogLine>> splitByPauses(List<DialogLine> lines, double pause) {
        List<List<DialogLine>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        DialogLine prev = null;
        for (DialogLine line : lines) {
            if (prev != null) {
                double diff = line.start() - prev.end();
                if (diff >= pause) {
                    result.add(new ArrayList<>());
                }
            }
            result.get(result.size() - 1).add(line);
            prev = line;
        }
        return result;
    }

    private static List<List<DialogLine>> splitByCount(List<DialogLine> lines, int portion) {
        List<List<DialogLine>> result = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            List<DialogLine> group = new ArrayList<>();
            for (int j = 0; j < portion && i < lines.size(); j++, i++) {
                DialogLine line = lines.get(i);
                group.add(line);
            }
            result.add(group);
        }
        return result;
    }

    private static DialogLine joinLines(List<DialogLine> lines, int mainIndex,
                                        Double silenceBefore, String lineBefore, Double nextStart,
                                        IntPredicate useColor, Function<Boolean, String> getColor) {
        Map<String, String> fields1 = lines.get(mainIndex).fields();
        StringBuilder buf = new StringBuilder();
        double start = lines.get(mainIndex).start();
        if (silenceBefore != null) {
            start -= silenceBefore.doubleValue();
            Util.appendK(buf, "K", silenceBefore.doubleValue());
            if (lineBefore != null) {
                buf.append(lineBefore);
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            DialogLine line = lines.get(i);
            if (i > 0) {
                buf.append("\\N");
            }
            if (i == mainIndex) {
                buf.append(line.text());
                if (nextStart != null) {
                    double gap = nextStart.doubleValue() - (line.start() + line.sumLen());
                    Util.appendK(buf, gap);
                }
            } else if (line != null) {
                boolean color = useColor.test(i);
                if (color) {
                    buf.append("{\\c" + getColor.apply(true) + "&}");
                }
                buf.append(line.rawText());
                if (color) {
                    buf.append("{\\c" + getColor.apply(false) + "&}");
                }
            } else {
                buf.append("\\h");
            }
        }
        String text = buf.toString();
        double end = nextStart == null ? lines.get(mainIndex).end() : nextStart.doubleValue();
        return DialogLine.create(fields1, start, end, text);
    }

    private static List<DialogLine> subList(List<DialogLine> lines, int from, int to) {
        List<DialogLine> result = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            if (i < 0 || i >= lines.size()) {
                result.add(null);
            } else {
                result.add(lines.get(i));
            }
        }
        return result;
    }

    private static List<DialogLine> join(List<DialogLine> l1, List<DialogLine> l2) {
        List<DialogLine> result = new ArrayList<>();
        result.addAll(l1);
        result.addAll(l2);
        return result;
    }

    private static List<DialogLine> empty(int n) {
        return Collections.nCopies(n, null);
    }

    public static List<DialogLine> join(ParsedAss parsed) {
        List<List<DialogLine>> groups = splitByPauses(parsed.getLines(), 2.5); // todo: extract to parameter
        List<DialogLine> newLines = new ArrayList<>();
        double prevEnd = 0;
        for (int gi = 0; gi < groups.size(); gi++) {
            List<DialogLine> group = groups.get(gi);
            for (int i = 0; i < group.size(); i++) {
                DialogLine line = group.get(i);
                Double silenceBefore;
                String lineBefore;
                if (i == 0) {
                    double sincePrev = line.start() - prevEnd;
                    double addSilence;
                    if (gi == 0) {
                        addSilence = 2.0; // todo: extract to parameter
                        lineBefore = "----";
                    } else {
                        addSilence = 0.75; // todo: extract to parameter
                        lineBefore = null;
                    }
                    silenceBefore = Math.min(addSilence, sincePrev);
                } else {
                    silenceBefore = null;
                    lineBefore = null;
                }
                int rem = i % 4;
                List<DialogLine> join = switch (rem) {
                    case 0 -> join(subList(group, i, i + 1), empty(2));
                    case 1 -> subList(group, i - 1, i + 2);
                    case 2 -> join(empty(2), subList(group, i, i + 1));
                    case 3 -> join(subList(group, i + 1, i + 2), subList(group, i - 1, i));
                    default -> Collections.emptyList();
                };
                String styleName = line.fields().getOrDefault("Style", "Default");
                AssStyle style = parsed.styles.styles.get(styleName);
                String primary;
                String secondary;
                if (style != null) {
                    primary = style.values().get(AssStyleKey.PrimaryColour);
                    secondary = style.values().get(AssStyleKey.SecondaryColour);
                } else {
                    primary = "&HFFFFFF";
                    secondary = "&H0000FF";
                }
                Double nextStart;
                if (i + 1 < group.size()) {
                    DialogLine next = group.get(i + 1);
                    nextStart = next.start();
                } else {
                    nextStart = null;
                }
                DialogLine newLine = joinLines(
                    join, rem, silenceBefore, lineBefore, nextStart,
                    j -> rem == 3 && (j == 0 || j == 1),
                    before -> before.booleanValue() ? secondary : primary
                );
                newLines.add(newLine);
                prevEnd = newLine.end();
            }
        }
        return newLines;
    }

    public static void join(Path origAssPath, Path newAssPath) throws IOException {
        ParsedAss origAss = AssParser.parse(origAssPath);
        List<DialogLine> newLines = join(origAss);
        ParsedAss newAss = origAss.withLines(newLines);
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(newAssPath))) {
            newAss.write(pw);
        }
    }
}
