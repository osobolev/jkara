package jkara.sync;

import jkara.Segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class RealSegments {

    private final RealText real;
    private final FastResult fast;

    private Integer prevSegment = null;
    private int start = -1;

    private final List<Segment> result = new ArrayList<>();

    RealSegments(RealText real, FastResult fast) {
        this.real = real;
        this.fast = fast;
    }

    private void addChunk(int to) {
        if (prevSegment == null)
            return;
        StringBuilder buf = new StringBuilder();
        for (int i = start; i < to; i++) {
            CWS ch = real.list.get(i);
            buf.append(ch.ch);
        }
        String origText = buf.toString().trim();
        Segment fastSegment = fast.segments.get(prevSegment.intValue());
        result.add(new Segment(fastSegment.start(), fastSegment.end(), origText));
    }

    List<Segment> split() {
        for (int i = 0; i < real.list.size(); i++) {
            CWS ch = real.list.get(i);
            Integer segment = ch.segment;
            if (!Objects.equals(prevSegment, segment)) {
                addChunk(i);
                start = i;
            }
            prevSegment = segment;
        }
        addChunk(real.list.size());
        return result;
    }
}
