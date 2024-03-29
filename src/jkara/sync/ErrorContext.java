package jkara.sync;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class ErrorContext {

    private final Path text;
    private final Path fastJson;
    private final List<CWS> realText;
    private final FastResult fastResult;

    ErrorContext(Path text, Path fastJson, List<CWS> realText, FastResult fastResult) {
        this.text = text;
        this.fastJson = fastJson;
        this.realText = realText;
        this.fastResult = fastResult;
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

    private void syncError(Path file, String ctx) throws SyncException {
        throw new SyncException(String.format("Cannot sync at %s in file %s", ctx, file.getFileName()), fastJson);
    }

    void fastError(int fastPosition) throws SyncException {
        String ctx = getErrorContext(fastPosition);
        syncError(fastJson, ctx);
    }

    void realError(int realPosition) throws SyncException {
        String ctx = getFallbackErrorContext(realText, realPosition);
        syncError(text, ctx);
    }
}
