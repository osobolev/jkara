package jkara.ass;

import jkara.util.Util;
import smalljson.JSONArray;
import smalljson.JSONObject;
import smalljson.JSONRuntimeException;

import java.io.IOException;
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
        return list.get(i).ch;
    }

    private static double getDouble(JSONObject cseg, String key, int i, JSONObject seg) {
        Double value = cseg.opt(key, double.class);
        if (value == null || value.isNaN()) {
            String text = seg.opt("text", String.class);
            String where = text == null ? "segment " + i : "'" + text + "'";
            throw new JSONRuntimeException(String.format("Missing \"%s\" field at %s", key, where));
        }
        return value.doubleValue();
    }

    static Aligned read(Path file) throws IOException {
        List<CSegment> buf = new ArrayList<>();
        JSONObject obj = Util.JSON.parseObject(file);
        JSONArray segs = obj.get("segments", JSONArray.class);
        for (int i = 0; i < segs.length(); i++) {
            JSONObject seg = segs.get(i, JSONObject.class);
            JSONArray csegs = seg.get("char-segments", JSONArray.class);
            for (int j = 0; j < csegs.length(); j++) {
                JSONObject cseg = csegs.get(j, JSONObject.class);
                double start = getDouble(cseg, "start", i, seg);
                double end = getDouble(cseg, "end", i, seg);
                char ch = cseg.get("char", String.class).charAt(0);
                buf.add(new CSegment(ch, Timestamps.create(start, end)));
            }
        }
        return new Aligned(buf);
    }
}
