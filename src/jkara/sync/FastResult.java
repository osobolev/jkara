package jkara.sync;

import jkara.Segment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class FastResult {

    final List<CWI> list;
    final List<Segment> segments;

    private FastResult(List<CWI> list, List<Segment> segments) {
        this.list = list;
        this.segments = segments;
    }

    static FastResult read(Path file) throws IOException {
        List<CWI> buf = new ArrayList<>();
        List<Segment> segments = new ArrayList<>();
        try (InputStream is = Files.newInputStream(file)) {
            JSONObject obj = new JSONObject(new JSONTokener(is));
            JSONArray segs = obj.getJSONArray("segments");
            for (int i = 0; i < segs.length(); i++) {
                JSONObject seg = segs.getJSONObject(i);
                double start = seg.getDouble("start");
                double end = seg.getDouble("end");
                String text = seg.getString("text");
                int segmentIndex = segments.size();
                segments.add(new Segment(start, end, text));
                if (i > 0) {
                    Normalizer.append(buf, " ", j -> null);
                }
                Normalizer.append(buf, text, j -> segmentIndex);
            }
        }
        Normalizer.finish(buf);
        return new FastResult(buf, segments);
    }
}
