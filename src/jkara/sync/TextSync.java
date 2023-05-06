package jkara.sync;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffAlgorithmFactory;
import com.github.difflib.algorithm.myers.MeyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import jkara.OutputFactory;
import jkara.Segment;
import jkara.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;

public final class TextSync {

    private static Integer segment(Iterable<CWI> fastChars) {
        Set<Integer> segments = new HashSet<>();
        for (CWI ch : fastChars) {
            Integer segment = ch.segment;
            if (segment == null)
                return null;
            segments.add(segment);
        }
        if (segments.size() == 1)
            return segments.iterator().next();
        return null;
    }

    private static void setSegment(Integer segment, Iterable<CWI> chars) {
        if (segment == null)
            return;
        for (CWI ch : chars) {
            ch.segment = segment;
        }
    }

    static void align(List<CWI> real, List<CWI> fast) {
        DiffAlgorithmFactory factory = MeyersDiff.factory();
        Patch<CWI> diff = DiffUtils.diff(fast, real, factory.create((cw1, cw2) -> cw1.ch == cw2.ch), null, true);
        for (AbstractDelta<CWI> delta : diff.getDeltas()) {
            switch (delta.getType()) {
            case INSERT: {
                Chunk<CWI> fastChunk = delta.getSource();
                Chunk<CWI> realChunk = delta.getTarget();
                int position = fastChunk.getPosition();
                Integer segment;
                if (position > 0 && position < fast.size()) {
                    CWI before = fast.get(position - 1);
                    CWI after = fast.get(position);
                    segment = segment(Arrays.asList(before, after));
                } else {
                    segment = null;
                }
                setSegment(segment, realChunk.getLines());
                break;
            }
            case DELETE:
                break;
            case CHANGE: {
                Chunk<CWI> fastChunk = delta.getSource();
                Chunk<CWI> realChunk = delta.getTarget();
                Integer segment = segment(fastChunk.getLines());
                setSegment(segment, realChunk.getLines());
                break;
            }
            case EQUAL: {
                Chunk<CWI> fastChunk = delta.getSource();
                Chunk<CWI> realChunk = delta.getTarget();
                for (int i = 0; i < fastChunk.getLines().size(); i++) {
                    CWI ifast = fastChunk.getLines().get(i);
                    CWI ireal = realChunk.getLines().get(i);
                    Integer segment = ifast.segment;
                    if (segment != null) {
                        ireal.segment = segment;
                    }
                }
            }
            }
        }
        for (CWI ch : real) {
            if (ch.segment == null) {
                if (ch.ch != ' ') {
                    // todo: error
                    System.out.println(ch);
                    throw new IllegalStateException();
                }
            }
        }
    }

    private static List<Segment> getResult(RealText real, FastResult fast) {
        return new RealSegments(real, fast).split();
    }

    private static String sanitize(String str) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Util.isLetter(ch)) {
                buf.append(ch);
            } else if (i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                if (next > ' ') {
                    buf.append(' ');
                }
            }
        }
        return buf.toString();
    }

    public static void sync(Path text, Path fastJson, OutputFactory textJson) throws IOException {
        RealText real = RealText.read(text);
        FastResult fast = FastResult.read(fastJson);

        align(real.list, fast.list);
        List<Segment> segments = getResult(real, fast);
        JSONArray array = new JSONArray();
        for (Segment segment : segments) {
            JSONObject obj = new JSONObject(Map.of(
                "start", segment.start(),
                "end", segment.end(),
                "text", sanitize(segment.text())
            ));
            array.put(obj);
        }
        JSONObject root = new JSONObject(Map.of(
            "segments", array,
            "language", fast.language
        ));
        try (Writer w = textJson.open()) {
            root.write(w, 2, 0);
        }
    }

    public static void main(String[] args) throws IOException {
        Path real = Path.of("C:\\home\\projects\\my\\kara\\work\\text.txt");
        Path fast = Path.of("C:\\home\\projects\\my\\kara\\work\\_fast.json");
//        Path real = Path.of("C:\\Downloads\\kara\\war\\text.txt");
//        Path fast = Path.of("C:\\Downloads\\kara\\war\\fast.json");

        StringWriter sw = new StringWriter();
        sync(real, fast, () -> sw);
        System.out.println(sw);
    }
}
