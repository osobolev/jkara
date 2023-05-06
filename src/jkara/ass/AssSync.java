package jkara.ass;

import jkara.util.OutputFactory;
import jkara.util.Util;

import java.io.IOException;
import java.nio.file.Path;

public final class AssSync {

    public static void sync(Path text, Path alignedJson, OutputFactory factory) throws IOException {
        RawText raw = RawText.read(text);
        Aligned aligned = Aligned.read(alignedJson);
        int ir = 0;
        int ia = 0;
        while (true) {
            while (ir < raw.size()) {
                char cr = raw.get(ir);
                if (Util.isLetter(cr))
                    break;
                ir++;
            }
            if (ir >= raw.size())
                break;
            while (ia < aligned.size()) {
                char ca = aligned.get(ia);
                if (Util.isLetter(ca))
                    break;
                ia++;
            }
            if (ia >= aligned.size())
                break;
            char cr = raw.get(ir);
            CSegment ca = aligned.list.get(ia);
            if (!String.valueOf(ca.ch).equalsIgnoreCase(String.valueOf(cr)))
                throw new IllegalStateException();

            raw.chars[ir].timestamps = ca.timestamps;
            ir++;
            ia++;
        }
        AssWriter.write(raw, factory);
    }
}
