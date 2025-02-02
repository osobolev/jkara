package jkara.pipe;

import jkara.util.Util;
import smalljson.JSONFactory;
import smalljson.JSONObject;
import smalljson.parser.JSONParser;
import smalljson.parser.JSONToken;
import smalljson.parser.JSONTokenType;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

final class CutRange {

    private final Double start;
    private final Double end;

    private CutRange(Double start, Double end) {
        this.start = start;
        this.end = end;
    }

    private static Map<String, String> queryParams(String url) {
        Map<String, String> params = new HashMap<>();
        int q = url.indexOf('?');
        if (q < 0)
            return params;
        String[] pairs = url.substring(q + 1).split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0)
                continue;
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private static int parse(StringBuilder buf, String unit) {
        int pos = buf.indexOf(unit);
        if (pos > 0) {
            int value = Integer.parseInt(buf.substring(0, pos));
            buf.delete(0, pos + 1);
            return value;
        } else {
            return 0;
        }
    }

    private static double parseTime(String t) {
        StringBuilder buf = new StringBuilder(t.trim());
        long hours = parse(buf, "h");
        long minutes = parse(buf, "m");
        long seconds = parse(buf, "s");
        if (!buf.isEmpty() && hours == 0 && minutes == 0 && seconds == 0) {
            return Integer.parseInt(buf.toString());
        }
        return hours * 60 * 60 + minutes * 60 + seconds;
    }

    static CutRange create(String url) {
        Map<String, String> params = queryParams(url);
        String t = params.get("t");
        String start = params.get("start");
        String end = params.get("end");
        Double secStart = null;
        if (t != null) {
            secStart = parseTime(t);
        } else if (start != null) {
            secStart = parseTime(start);
        }
        Double secEnd = null;
        if (end != null) {
            secEnd = parseTime(end);
        }
        if (secStart == null && secEnd == null)
            return null;
        return new CutRange(secStart, secEnd);
    }

    private final class FrameAcc {

        private Double lastBeforeStart = null;
        private Double firstAfterEnd = null;

        void add(double ts) {
            if (start != null) {
                if (ts <= start.doubleValue() && (lastBeforeStart == null || ts > lastBeforeStart.doubleValue())) {
                    lastBeforeStart = ts;
                }
            }
            if (end != null) {
                if (firstAfterEnd == null && ts >= end.doubleValue()) {
                    firstAfterEnd = ts;
                }
            }
        }
    }

    private static Double getFrameTime(Double time, Function<FrameAcc, Double> getAccTime, FrameAcc keyAcc, FrameAcc nonKeyAcc) {
        if (time == null)
            return null;
        FrameAcc[] accs = {keyAcc, nonKeyAcc};
        for (FrameAcc acc : accs) {
            Double accTime = getAccTime.apply(acc);
            if (accTime != null)
                return accTime;
        }
        return time;
    }

    private static boolean skipTo(JSONParser tok, JSONTokenType type) {
        while (true) {
            JSONToken t = tok.getCurrent();
            if (t.type == JSONTokenType.EOF)
                return false;
            if (t.type == type) {
                tok.next();
                return true;
            }
            tok.next();
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private CutRange parseFrameStream(double duration, InputStream in) {
        JSONParser tok = Util.JSON.newParser(JSONFactory.toFast(in));
        if (!skipTo(tok, JSONTokenType.LSQUARE))
            return this;
        FrameAcc keyAcc = new FrameAcc();
        FrameAcc nonKeyAcc = new FrameAcc();
        long prevPercent = -1;
        while (tok.getCurrent().type != JSONTokenType.EOF) {
            JSONObject frame = tok.parseObject();
            try {
                double ts = frame.get("best_effort_timestamp_time", double.class).doubleValue();
                long percent = Math.round(ts / duration * 100.0);
                if (percent != prevPercent) {
                    System.out.printf("Scanning frames: %s%%\r", percent);
                    prevPercent = percent;
                }
                String type = frame.get("pict_type", String.class);
                if ("I".equalsIgnoreCase(type)) {
                    keyAcc.add(ts);
                } else {
                    nonKeyAcc.add(ts);
                }
            } catch (Exception ex) {
                // ignore
            }
            if (!skipTo(tok, JSONTokenType.COMMA))
                break;
        }
        System.out.println();
        Double realStart = getFrameTime(start, acc -> acc.lastBeforeStart, keyAcc, nonKeyAcc);
        Double realEnd = getFrameTime(end, acc -> acc.firstAfterEnd, keyAcc, nonKeyAcc);
        return new CutRange(realStart, realEnd);
    }

    void doCut(ProcRunner runner, Path file, Path outFile) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of(
            "-y",
            "-i", file.toString()
        ));
        if (start != null) {
            args.addAll(List.of(
                "-ss", start.toString()
            ));
        }
        if (end != null) {
            args.addAll(List.of(
                "-to", end.toString()
            ));
        }
        args.addAll(List.of(
            "-c:v", "copy",
            "-c:a", "copy",
            "-avoid_negative_ts", "make_zero",
            outFile.toString()
        ));
        runner.runFFMPEG(args);
    }

    CutRange getRealCut(ProcRunner runner, Path file) throws IOException, InterruptedException {
        double duration;
        {
            JSONObject obj = runner.runFFProbe(List.of(
                "-print_format", "json",
                "-show_entries", "format=duration",
                file.toString()
            ));
            duration = obj.get("format", JSONObject.class).get("duration", double.class).doubleValue();
        }
        {
            AtomicReference<CutRange> realCut = new AtomicReference<>(this);
            try (PipedInputStream sink = new PipedInputStream()) {
                Thread sinkReader = new Thread(() -> realCut.set(parseFrameStream(duration, sink)));

                runner.runFFProbe(List.of(
                    "-print_format", "json",
                    "-select_streams", "v",
                    "-skip_frame", "nokey",
                    "-show_frames",
                    "-show_entries", "frame=best_effort_timestamp_time,pict_type",
                    file.toString()
                ), p -> {
                    try (PipedOutputStream out = new PipedOutputStream(sink)) {
                        sinkReader.start();
                        p.getInputStream().transferTo(out);
                    } catch (IOException ex) {
                        // ignore
                    }
                }, null);

                sinkReader.join();
            }
            return realCut.get();
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (start != null) {
            buf.append(duration(Math.round(start.doubleValue() * 1000.0)));
        } else {
            buf.append(duration(0L));
        }
        buf.append('-');
        if (end != null) {
            buf.append(duration(Math.round(end.doubleValue() * 1000.0)));
        } else {
            buf.append("inf");
        }
        return buf.toString();
    }

    static String duration(long millis) {
        long totalSeconds = millis / 1000;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long hours = totalMinutes / 60;
        if (hours > 0) {
            return String.format("%s:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%s:%02d", minutes, seconds);
        }
    }
}
