package jkara.ass;

import jkara.Segment;
import jkara.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class AssSync {

    public static void main(String[] args) throws IOException {
        RawText raw = RawText.read(Path.of("C:\\home\\projects\\my\\kara\\work\\text.txt"));
        Aligned aligned = Aligned.read(Path.of("C:\\home\\projects\\my\\kara\\work\\aligned.json"));
        System.out.println(raw.list);
//        RealText real = RealText.read(Path.of("C:\\Downloads\\kara\\war\\text.txt"));
//        Aligned aligned = Aligned.read(Path.of("C:\\Downloads\\kara\\war\\aligned.json"));
        int ir = 0;
        int ia = 0;
        while (true) {
            int ir0 = ir;
            while (ir < raw.list.size()) {
                String cr = raw.list.get(ir);
                if (Util.isLetter(cr.charAt(0)))
                    break;
                ir++;
            }
            if (ir >= raw.list.size())
                break;
            int ia0 = ia;
            while (ia < aligned.list.size()) {
                Segment ca = aligned.list.get(ia);
                if (Util.isLetter(ca.text().charAt(0)))
                    break;
                ia++;
            }
            if (ia >= aligned.list.size()) {
                // todo: assign some time to tail of real???
                break;
            }
            if (ir > ir0) {
                List<String> skipped = raw.list.subList(ir0, ir);
                // todo: assign some times to them too???
            }
            String cr = raw.list.get(ir);
            Segment ca = aligned.list.get(ia);
            if (!ca.text().equalsIgnoreCase(cr))
                throw new IllegalStateException();
            ir++;
            ia++;
        }
    }
}
