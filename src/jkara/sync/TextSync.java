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

    private final FastResult fastResult;

    private TextSync(FastResult fastResult) {
        this.fastResult = fastResult;
    }

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

    private Integer getErrorSegment(int fastPosition) {
        List<CWS> fast = fastResult.list;
        if (fastPosition < fast.size()) {
            CWS ch = fast.get(fastPosition);
            if (ch.segment != null)
                return ch.segment;
        }
        if (fastPosition > 0 && fastPosition + 1 < fast.size()) {
            CWS before = fast.get(fastPosition - 1);
            CWS after = fast.get(fastPosition + 1);
            if (before.segment != null && after.segment != null && Objects.equals(before.segment, after.segment))
                return before.segment;
        }
        if (fastPosition > 0) {
            CWS before = fast.get(fastPosition - 1);
            if (before.segment != null)
                return before.segment;
        }
        if (fastPosition + 1 < fast.size()) {
            CWS after = fast.get(fastPosition + 1);
            if (after.segment != null)
                return after.segment;
        }
        return null;
    }

    private static String getFallbackErrorContext(List<CWS> chars, int position) {
        StringBuilder buf = new StringBuilder();
        for (int i = position - 15; i <= position + 15; i++) {
            if (i >= 0 && i < chars.size()) {
                CWS ch = chars.get(i);
                buf.appendCodePoint(ch.ch);
            }
        }
        if (buf.isEmpty())
            return "<unknown>";
        return "'" + buf + "'";
    }

    private String getErrorContext(int fastPosition) {
        Integer errorSegment = getErrorSegment(fastPosition);
        if (errorSegment != null) {
            Segment segment = fastResult.segments.get(errorSegment.intValue());
            return String.format("'%s' (index %d)", segment.text(), segment.index());
        }
        return getFallbackErrorContext(fastResult.list, fastPosition);
    }

    private static void syncError(String ctx) throws SyncException {
        throw new SyncException(String.format("Cannot sync at %s", ctx));
    }

    private void setSegment(Integer segment, List<CWS> chars, int fastPosition) throws SyncException {
        if (segment == null) {
            boolean hasText = chars.stream().anyMatch(ch -> ch.ch != ' ');
            if (hasText) {
                String ctx = getErrorContext(fastPosition);
                syncError(ctx);
            }
            return;
        }
        for (CWS ch : chars) {
            ch.segment = segment;
        }
    }

    private void align(List<CWS> real, List<CWS> fast) throws SyncException {
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
                setSegment(segment, realChunk.getLines(), position);
                break;
            }
            case DELETE:
                break;
            case CHANGE: {
                Chunk<CWS> fastChunk = delta.getSource();
                Chunk<CWS> realChunk = delta.getTarget();
                Integer segment = segment(fastChunk.getLines());
                setSegment(segment, realChunk.getLines(), fastChunk.getPosition());
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
        for (int i = 0; i < real.size(); i++) {
            CWS ch = real.get(i);
            if (ch.segment == null) {
                if (ch.ch != ' ') {
                    String ctx = getFallbackErrorContext(real, i);
                    syncError(ctx);
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

        new TextSync(fast).align(real.list, fast.list);
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
