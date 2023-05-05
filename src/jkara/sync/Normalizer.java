package jkara.sync;

import jkara.Util;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

final class Normalizer {

    static void append(List<CWI> buf, String str, IntFunction<Integer> getPayload) {
        for (int j = 0; j < str.length(); j++) {
            char ch = str.charAt(j);
            if (Util.isLetter(ch)) {
                char lch = Character.toLowerCase(ch);
                Integer index = getPayload.apply(j);
                buf.add(new CWI(lch, index));
            } else if (!buf.isEmpty()) {
                CWI last = buf.get(buf.size() - 1);
                if (last.ch() != ' ') {
                    Integer index = getPayload.apply(j);
                    buf.add(new CWI(' ', index));
                }
            }
        }
    }

    static void finish(List<CWI> buf) {
        if (buf.isEmpty())
            return;
        int lastIndex = buf.size() - 1;
        CWI last = buf.get(lastIndex);
        if (last.ch() == ' ') {
            buf.remove(lastIndex);
        }
    }

    static String plain(List<CWI> list) {
        return list.stream().map(e -> String.valueOf(e.ch())).collect(Collectors.joining());
    }
}
