package jkara.sync;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffAlgorithmFactory;
import com.github.difflib.algorithm.myers.MeyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import jkara.util.OutputFactory;
import jkara.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;

public final class TextSync {

    private static Integer segment(Iterable<CWS> fastChars) {
        Set<Integer> segments = new HashSet<>();
        for (CWS ch : fastChars) {
            Integer segment = ch.segment;
            if (segment == null)
                return null;
            segments.add(segment);
        }
        if (segments.size() == 1)
            return segments.iterator().next();
        return null;
    }

    private static void setSegment(Integer segment, Iterable<CWS> chars) {
        if (segment == null)
            return;
        for (CWS ch : chars) {
            ch.segment = segment;
        }
    }

    static void align(List<CWS> real, List<CWS> fast) throws SyncException {
        DiffAlgorithmFactory factory = MeyersDiff.factory();
        Patch<CWS> diff = DiffUtils.diff(fast, real, factory.create((cw1, cw2) -> cw1.ch == cw2.ch), null, true);
        for (AbstractDelta<CWS> delta : diff.getDeltas()) {
            switch (delta.getType()) {
            case INSERT: {
                Chunk<CWS> fastChunk = delta.getSource();
                Chunk<CWS> realChunk = delta.getTarget();
                int position = fastChunk.getPosition();
                Integer segment;
                if (position > 0 && position < fast.size()) {
                    CWS before = fast.get(position - 1);
                    CWS after = fast.get(position);
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
                Chunk<CWS> fastChunk = delta.getSource();
                Chunk<CWS> realChunk = delta.getTarget();
                Integer segment = segment(fastChunk.getLines());
                setSegment(segment, realChunk.getLines());
                break;
            }
            case EQUAL: {
                Chunk<CWS> fastChunk = delta.getSource();
                Chunk<CWS> realChunk = delta.getTarget();
                for (int i = 0; i < fastChunk.getLines().size(); i++) {
                    CWS ifast = fastChunk.getLines().get(i);
                    CWS ireal = realChunk.getLines().get(i);
                    Integer segment = ifast.segment;
                    if (segment != null) {
                        ireal.segment = segment;
                    }
                }
            }
            }
        }
        for (CWS ch : real) {
            if (ch.segment == null) {
                if (ch.ch != ' ') {
                    // todo: error text with details
                    throw new SyncException("Cannot sync!!!");
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

    public static void sync(Path text, Path fastJson, OutputFactory textJson) throws IOException, SyncException {
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
}
