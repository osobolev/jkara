package jkara.sync;

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

    final List<CWS> list;
    final List<Segment> segments;
    final String language;

    private FastResult(List<CWS> list, List<Segment> segments, String language) {
        this.list = list;
        this.segments = segments;
        this.language = language;
    }

    static FastResult read(Path file) throws IOException {
        List<CWS> buf = new ArrayList<>();
        List<Segment> segments = new ArrayList<>();
        String language;
        try (InputStream is = Files.newInputStream(file)) {
            JSONObject obj = new JSONObject(new JSONTokener(is));
            JSONArray segs = obj.getJSONArray("segments");
            language = obj.getString("language");
            for (int i = 0; i < segs.length(); i++) {
                JSONObject seg = segs.getJSONObject(i);
                double start = seg.getDouble("start");
                double end = seg.getDouble("end");
                String text = seg.getString("text");
                segments.add(new Segment(i, start, end, text));
                if (i > 0) {
                    Normalizer.append(buf, " ", null);
                }
                Normalizer.append(buf, text, i);
            }
        }
        Normalizer.finish(buf);
        return new FastResult(buf, segments, language);
    }
}
