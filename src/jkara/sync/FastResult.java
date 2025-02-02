package jkara.sync;

import jkara.util.Util;
import smalljson.JSONArray;
import smalljson.JSONObject;

import java.io.IOException;
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
        JSONObject obj = Util.JSON.parseObject(file);
        JSONArray segs = obj.get("segments", JSONArray.class);
        String language = obj.get("language", String.class);
        for (int i = 0; i < segs.length(); i++) {
            JSONObject seg = segs.get(i, JSONObject.class);
            double start = seg.get("start", double.class).doubleValue();
            double end = seg.get("end", double.class).doubleValue();
            String text = seg.get("text", String.class);
            segments.add(new Segment(i, start, end, text));
            if (i > 0) {
                Normalizer.append(buf, " ", null);
            }
            Normalizer.append(buf, text, i);
        }
        Normalizer.finish(buf);
        return new FastResult(buf, segments, language);
    }
}
