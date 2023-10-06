package jkara.ass;

import ass.model.DialogLine;
import jkara.util.OutputFactory;
import jkara.util.Util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class AssWriter {

    private static final String HEADER = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 384
        PlayResY: 288
        ScaledBorderAndShadow: yes
            
        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H000000FF,&H00FFFFFF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,5,10,10,10,1
            
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

    private static void append(StringBuilder buf, double start, double end) {
        Util.appendK(buf, end - start);
    }

    private static String assLine(List<CSegment> line) {
        double minStart = Double.NaN;
        double maxEnd = Double.NaN;
        for (CSegment ch : line) {
            Timestamps ts = ch.timestamps;
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
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < line.size()) {
            CSegment ch = line.get(i);
            Timestamps ts = ch.timestamps;
            if (ts == null) {
                if (i > 0) {
                    double prevEnd = line.get(i - 1).timestamps.end();
                    StringBuilder spaces = new StringBuilder();
                    double end = maxEnd;
                    while (i < line.size()) {
                        CSegment chi = line.get(i);
                        if (chi.timestamps != null) {
                            end = chi.timestamps.start();
                            break;
                        }
                        spaces.append(chi.ch);
                        i++;
                    }
                    append(buf, prevEnd, end);
                    buf.append(spaces);
                } else {
                    buf.append(ch.ch);
                    i++;
                }
            } else {
                append(buf, ts.start(), ts.end());
                buf.append(ch.ch);
                i++;
            }
        }
        return assLine(minStart, maxEnd, buf.toString());
    }

    static String assLine(double start, double end, String text) {
        return String.format(
            "Dialogue: 0,%s,%s,Default,,0,0,0,,%s",
            DialogLine.formatTimestamp(start), DialogLine.formatTimestamp(end), text
        );
    }

    static void write(RawText text, OutputFactory factory) throws IOException {
        List<List<CSegment>> lines = splitToLines(text);
        write(factory, lines.stream().map(AssWriter::assLine));
    }

    static void write(OutputFactory factory, Stream<String> lines) throws IOException {
        try (PrintWriter pw = new PrintWriter(factory.open())) {
            HEADER.lines().forEach(pw::println);
            lines.filter(Objects::nonNull).forEach(pw::println);
        }
    }
}
