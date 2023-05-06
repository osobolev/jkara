package jkara.scroll;

import jkara.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class AssJoiner {

    // todo: нужно сгруппировать AssLines по принципу "расстояние по времени между соседними строками < 1 сек"
    // todo: потом эти группы побить на группы с фиксированным кол-вом строк (например 4)
    // todo: показываем так:
    //   *1  1     5 *5  5 ...
    //    2 *2     6  6 *6
    //       3 *3  3     7
    //       4  4 *4     8

    static List<List<AssLine>> splitByPauses(List<AssLine> lines, double pause) {
        List<List<AssLine>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        AssLine prev = null;
        for (AssLine line : lines) {
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

    static List<List<AssLine>> splitByCount(List<AssLine> lines, int portion) {
        List<List<AssLine>> result = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            List<AssLine> group = new ArrayList<>();
            for (int j = 0; j < portion && i < lines.size(); j++, i++) {
                AssLine line = lines.get(i);
                group.add(line);
            }
            result.add(group);
        }
        return result;
    }

    static AssLine join(List<AssLine> lines) {
        String[] fields1 = lines.get(0).fields();
        String[] fields = Arrays.copyOf(fields1, fields1.length);
        StringBuilder buf = new StringBuilder();
        AssLine prev = null;
        double start = lines.get(0).start();
        for (AssLine line : lines) {
            if (prev != null) {
                double gap = line.start() - (prev.start() + prev.sumLen());
                if (gap > 0) {
                    Util.appendK(buf, gap);
                }
                buf.append("\\N");
            }
            buf.append(line.text());
            prev = line;
        }
        String text = buf.toString();
        double end = lines.get(lines.size() - 1).end();
        fields[1] = Util.formatTimestamp(start);
        fields[2] = Util.formatTimestamp(end);
        return AssLine.create(fields, start, end, text);
    }
}
