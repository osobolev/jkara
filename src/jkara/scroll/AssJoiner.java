package jkara.scroll;

import ass.model.AssStyle;
import ass.model.AssStyleKey;
import ass.model.DialogLine;
import ass.model.ParsedAss;
import ass.parser.AssParser;
import jkara.opts.OKaraoke;
import jkara.util.Util;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
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

    private static List<DialogLine> shiftLines(List<DialogLine> lines, double shift) {
        if (shift == 0)
            return lines;
        return lines.stream().map(line -> new DialogLine(
            line.fields(), Math.max(line.start() - shift, 0), line.end() - shift,
            line.text(), line.rawText(), line.sumLen()
        )).toList();
    }

    public static List<DialogLine> join(ParsedAss parsed, List<String> titles, OKaraoke opts) {
        List<List<DialogLine>> groups = splitByPauses(shiftLines(parsed.getLines(), opts.shift()), opts.betweenGroups());
        List<DialogLine> newLines = new ArrayList<>();
        double prevEnd = 0;
        for (int gi = 0; gi < groups.size(); gi++) {
            List<DialogLine> group = groups.get(gi);
            for (int i = 0; i < group.size(); i++) {
                DialogLine line = group.get(i);
                Double silenceBefore;
                boolean starting;
                if (i == 0) {
                    double sincePrev = line.start() - prevEnd;
                    double addSilence;
                    if (gi == 0) {
                        addSilence = opts.preview1();
                        starting = true;
                        if (!titles.isEmpty()) {
                            double titlesTime = Math.min(line.start() - addSilence - opts.minAfterTitles(), opts.maxTitles());
                            if (titlesTime >= opts.minTitles()) {
                                DialogLine titleLine = DialogLine.create(line.fields(), 0.0, titlesTime, String.join("\\N", titles));
                                newLines.add(titleLine);
                            }
                        }
                    } else {
                        starting = sincePrev >= opts.minSoloLength();
                        if (starting) {
                            addSilence = opts.previewAfterSolo();
                        } else {
                            addSilence = opts.preview();
                        }
                    }
                    silenceBefore = Math.min(addSilence, sincePrev);
                } else {
                    silenceBefore = null;
                    starting = false;
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
                Map<AssStyleKey, String> styleValues = style == null ? Collections.emptyMap() : style.values();
                String primary = styleValues.getOrDefault(AssStyleKey.PrimaryColour, "&H0000FF");
                String secondary = styleValues.getOrDefault(AssStyleKey.SecondaryColour, "&HFFFFFF");
                Double nextStart;
                if (i + 1 < group.size()) {
                    DialogLine next = group.get(i + 1);
                    nextStart = next.start();
                } else {
                    nextStart = null;
                }
                String lineBefore = starting ? "---- " : null;
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

    public static void join(Path origAssPath, Path infoJson, Path newAssPath, OKaraoke opts) throws IOException {
        List<String> titles = new ArrayList<>();
        if (Files.exists(infoJson)) {
            try (InputStream is = Files.newInputStream(infoJson)) {
                JSONObject obj = new JSONObject(new JSONTokener(is));
                String artist = obj.optString("artist", null);
                String track = obj.optString("track", null);
                String title = obj.optString("title", null);
                String fullTitle = obj.optString("fulltitle", null);
                if (track != null) {
                    titles.add(track);
                    if (artist != null) {
                        titles.add("by " + artist);
                    }
                } else if (fullTitle != null) {
                    titles.add(fullTitle);
                } else if (title != null) {
                    titles.add(title);
                }
            }
        }
        ParsedAss origAss = AssParser.parse(origAssPath);
        List<DialogLine> newLines = join(origAss, titles, opts);
        ParsedAss newAss = origAss.withLines(newLines);
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(newAssPath))) {
            newAss.write(pw);
        }
    }
}
