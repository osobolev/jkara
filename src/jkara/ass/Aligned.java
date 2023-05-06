package jkara.ass;

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

    final List<CSegment> list;

    private Aligned(List<CSegment> list) {
        this.list = list;
    }

    int size() {
        return list.size();
    }

    char get(int i) {
        return list.get(i).ch();
    }

    static Aligned read(Path file) throws IOException {
        List<CSegment> buf = new ArrayList<>();
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
                    char ch = cseg.getString("char").charAt(0);
                    buf.add(new CSegment(start, end, ch));
                }
            }
        }
        return new Aligned(buf);
    }
}
