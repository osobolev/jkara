package jkara.sync;

import jkara.util.Util;

import java.util.List;
import java.util.function.BiConsumer;

final class Normalizer {

    static void append(List<CWS> buf, String str, Integer segment, BiConsumer<Integer, CWS> origMap) {
        for (int j = 0; j < str.length(); j++) {
            char ch = str.charAt(j);
            if (Util.isLetter(ch)) {
                char lch = Character.toLowerCase(ch);
                CWS cws = new CWS(lch, segment);
                origMap.accept(j, cws);
                buf.add(cws);
            } else if (!buf.isEmpty()) {
                CWS last = buf.get(buf.size() - 1);
                if (last.ch != ' ') {
                    CWS cws = new CWS(' ', segment);
                    origMap.accept(j, cws);
                    buf.add(cws);
                }
            }
        }
    }

    static void append(List<CWS> buf, String str, Integer segment) {
        append(buf, str, segment, (index, cws) -> {});
    }

    static void finish(List<CWS> buf) {
        if (buf.isEmpty())
            return;
        int lastIndex = buf.size() - 1;
        CWS last = buf.get(lastIndex);
        if (last.ch == ' ') {
            buf.remove(lastIndex);
        }
    }
}
