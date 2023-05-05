package jkara.sync;

import jkara.Segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
//        StringBuilder buf = new StringBuilder();
//        for (int i = start; i < to; i++) {
//            CWI ch = real.list.get(i);
//            buf.append(ch.ch());
//        }
        CWI first = real.list.get(start);
        CWI last = real.list.get(to - 1);
        int firstIndex = first.index().intValue();
        int lastIndex = last.index().intValue();
        // todo: no need to extract original text - send normalized to align???
        String origText = real.text.substring(firstIndex, lastIndex + 1).trim();
        Segment fastSegment = fast.segments.get(prevSegment.intValue());
        result.add(new Segment(fastSegment.start(), fastSegment.end(), origText));
    }

    List<Segment> split(Map<CWI, Integer> realSegments) {
        for (int i = 0; i < real.list.size(); i++) {
            CWI ch = real.list.get(i);
            Integer segment = realSegments.get(ch);
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
