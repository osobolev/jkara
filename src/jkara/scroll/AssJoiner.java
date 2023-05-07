package jkara.scroll;

import ass.model.DialogLine;
import jkara.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AssJoiner {

    // todo: нужно сгруппировать AssLines по принципу "расстояние по времени между соседними строками < 1 сек"
    // todo: потом эти группы побить на группы с фиксированным кол-вом строк (например 4)
    // todo: показываем так:
    //   *1  1     5 *5  5 ...
    //    2 *2     6  6 *6
    //       3 *3  3     7
    //       4  4 *4     8

    static List<List<DialogLine>> splitByPauses(List<DialogLine> lines, double pause) {
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

    static List<List<DialogLine>> splitByCount(List<DialogLine> lines, int portion) {
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

    static DialogLine join(List<DialogLine> lines) {
        Map<String, String> fields1 = lines.get(0).fields();
        StringBuilder buf = new StringBuilder();
        DialogLine prev = null;
        double start = lines.get(0).start();
        for (DialogLine line : lines) {
            if (prev != null) {
                double gap = line.start() - (prev.start() + prev.sumLen());
                Util.appendK(buf, gap);
                buf.append("\\N");
            }
            buf.append(line.text());
            prev = line;
        }
        String text = buf.toString();
        double end = lines.get(lines.size() - 1).end();
        return DialogLine.create(fields1, start, end, text);
    }

    static DialogLine join2(List<DialogLine> lines, int mainIndex) {
        Map<String, String> fields1 = lines.get(mainIndex).fields();
        DialogLine next;
        if (mainIndex + 1 < lines.size()) {
            next = lines.get(mainIndex + 1);
        } else {
            next = null;
        }
        StringBuilder buf = new StringBuilder();
        double start = lines.get(mainIndex).start();
        for (int i = 0; i < lines.size(); i++) {
            DialogLine line = lines.get(i);
            if (i > 0) {
                buf.append("\\N");
            }
            if (i == mainIndex) {
                buf.append(line.text());
                if (next != null) {
                    double gap = next.start() - (line.start() + line.sumLen());
                    Util.appendK(buf, gap);
                }
            } else if (line != null) {
                buf.append(line.rawText());
            } else {
                buf.append("\\h");
            }
        }
        String text = buf.toString();
        double end = next == null ? lines.get(mainIndex).end() : next.start();
        return DialogLine.create(fields1, start, end, text);
    }
}
