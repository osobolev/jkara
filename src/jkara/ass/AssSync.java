package jkara.ass;

import jkara.Util;

import java.io.IOException;
import java.nio.file.Path;

public final class AssSync {

    public static void sync(Path text, Path alignedJson) throws IOException {
        RawText raw = RawText.read(text);
        Aligned aligned = Aligned.read(alignedJson);
        int ir = 0;
        int ia = 0;
        while (true) {
            int ir0 = ir;
            while (ir < raw.size()) {
                char cr = raw.get(ir);
                if (Util.isLetter(cr))
                    break;
                ir++;
            }
            if (ir >= raw.size())
                break;
            int ia0 = ia;
            while (ia < aligned.size()) {
                char ca = aligned.get(ia);
                if (Util.isLetter(ca))
                    break;
                ia++;
            }
            if (ia >= aligned.size()) {
                // todo: assign some time to tail of real???
                break;
            }
            if (ir > ir0) {
                // todo: assign some times to them too???
            }
            char cr = raw.get(ir);
            CSegment ca = aligned.list.get(ia);
            if (!String.valueOf(ca.ch()).equalsIgnoreCase(String.valueOf(cr)))
                throw new IllegalStateException();

            if (!Double.isNaN(ca.start()) && !Double.isNaN(ca.end())) {
                raw.timestamps[ir] = new Timestamps(ca.start(), ca.end());
            }
            ir++;
            ia++;
        }
    }
}
