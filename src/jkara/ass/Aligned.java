package jkara.ass;

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

final class Aligned {

    final List<Segment> list;

    private Aligned(List<Segment> list) {
        this.list = list;
    }

    static Aligned read(Path file) throws IOException {
        List<Segment> buf = new ArrayList<>();
        try (InputStream is = Files.newInputStream(file)) {
            JSONObject obj = new JSONObject(new JSONTokener(is));
            JSONArray segs = obj.getJSONArray("segments");
            for (int i = 0; i < segs.length(); i++) {
                JSONObject seg = segs.getJSONObject(i);
                JSONArray csegs = seg.getJSONArray("char-segments");
                for (int j = 0; j < csegs.length(); j++) {
                    JSONObject cseg = csegs.getJSONObject(j);
                    double start = cseg.getDouble("start");
                    double end = cseg.getDouble("end");
                    String ch = cseg.getString("char");
                    buf.add(new Segment(start, end, ch));
                }
            }
        }
        return new Aligned(buf);
    }
}
