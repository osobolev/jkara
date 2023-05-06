package jkara.ass;

import jkara.util.OutputFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AssWriter {

    private static final String HEADER = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 384
        PlayResY: 288
        ScaledBorderAndShadow: yes
            
        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,2,2,10,10,10,1
            
        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    private static List<List<CSegment>> splitToLines(RawText text) {
        List<List<CSegment>> lines = new ArrayList<>();
        lines.add(new ArrayList<>());
        for (int i = 0; i < text.size(); i++) {
            char ch = text.get(i);
            if (ch == '\n') {
                lines.add(new ArrayList<>());
                continue;
            }
            List<CSegment> line = lines.get(lines.size() - 1);
            line.add(text.chars[i]);
        }
        return lines;
    }

    private static String formatTimestamp(double ts) {
        long totalSecs = Math.round(ts);
        double secs = totalSecs % 60 + (ts - totalSecs);
        long totalMins = totalSecs / 60;
        long mins = totalMins % 60;
        long hours = totalMins / 60;
        return String.format(Locale.ROOT, "%s:%02d:%05.2f", hours, mins, secs);
    }

    private static String assLine(List<CSegment> line) {
        double minStart = Double.NaN;
        double maxEnd = Double.NaN;
        StringBuilder buf = new StringBuilder();
        for (CSegment ch : line) {
            Timestamps ts = ch.timestamps;
            if (ts != null) {
                long k = Math.round((ts.end() - ts.start()) * 100);
                buf.append(String.format("{\\k%s}", k));
            }
            buf.append(ch.ch);
            if (ts == null)
                continue;
            if (Double.isNaN(minStart) || ts.start() < minStart) {
                minStart = ts.start();
            }
            if (Double.isNaN(maxEnd) || ts.end() > maxEnd) {
                maxEnd = ts.end();
            }
        }
        if (Double.isNaN(minStart) || Double.isNaN(maxEnd))
            return null;
        return String.format(
            "Dialogue: 0,%s,%s,Default,,0,0,0,,%s",
            formatTimestamp(minStart), formatTimestamp(maxEnd), buf
        );
    }

    static void write(RawText text, OutputFactory factory) throws IOException {
        List<List<CSegment>> lines = splitToLines(text);
        try (PrintWriter pw = new PrintWriter(factory.open())) {
            HEADER.lines().forEach(pw::println);
            for (List<CSegment> line : lines) {
                String assLine = assLine(line);
                if (assLine == null)
                    continue;
                pw.println(assLine);
            }
        }
    }
}
