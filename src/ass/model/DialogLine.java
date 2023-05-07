package ass.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record DialogLine(
    Map<String, String> fields,
    double start,
    double end,
    String text,
    String rawText,
    double sumLen
) {

    public static final String START = "Start";
    public static final String END = "End";
    public static final String TEXT = "Text";

    private static final Pattern K_TAG = Pattern.compile("\\\\\\s*k[a-z]*\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static DialogLine doCreate(Map<String, String> fields, double start, double end, String text) {
        StringBuilder buf = new StringBuilder();
        int inside = -1;
        int sumLen = 0;
        for (int j = 0; j < text.length(); j++) {
            char ch = text.charAt(j);
            if (inside >= 0) {
                if (ch == '}') {
                    String tag = text.substring(inside + 1, j).trim();
                    Matcher matcher = K_TAG.matcher(tag);
                    if (matcher.matches()) {
                        int len = Integer.parseInt(matcher.group(1));
                        sumLen += len;
                    }
                    inside = -1;
                }
            } else if (ch == '{') {
                inside = j;
            } else {
                buf.append(ch);
            }
        }
        String rawText = buf.toString();
        return new DialogLine(fields, start, end, text, rawText, sumLen / 100.0);
    }

    public static String formatTimestamp(double ts) {
        long totalSecs = (long) ts;
        double secs = totalSecs % 60 + (ts - totalSecs);
        long totalMins = totalSecs / 60;
        long mins = totalMins % 60;
        long hours = totalMins / 60;
        return String.format(Locale.ROOT, "%s:%02d:%05.2f", hours, mins, secs);
    }

    public static DialogLine create(Map<String, String> fields0, double start, double end, String text) {
        Objects.requireNonNull(text, "Missing text");
        Map<String, String> fields = new HashMap<>(fields0);
        fields.put(START, formatTimestamp(start));
        fields.put(END, formatTimestamp(end));
        fields.put(TEXT, text);
        return doCreate(fields, start, end, text);
    }

    private static final Pattern TS_FORMAT = Pattern.compile("(\\d+):(\\d+):(\\d+\\.\\d+)");

    public static double parseTimestamp(String str) {
        Objects.requireNonNull(str, "Missing timestamp");
        Matcher matcher = TS_FORMAT.matcher(str);
        if (!matcher.matches())
            throw new IllegalArgumentException("Wrong timestamp format: " + str);
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        double second = Double.parseDouble(matcher.group(3));
        return (hour * 60.0 + minute) * 60.0 + second;
    }

    public static DialogLine create(Map<String, String> fields) {
        double start = parseTimestamp(fields.get(START));
        double end = parseTimestamp(fields.get(END));
        String text = fields.getOrDefault(TEXT, "");
        return doCreate(fields, start, end, text);
    }

    public String formatAss(SectionFormat format) {
        StringBuilder buf = new StringBuilder("Dialogue: ");
        for (int i = 0; i < format.fields().size(); i++) {
            String field = format.fields().get(i);
            String value = fields.getOrDefault(field, "");
            if (i > 0) {
                buf.append(',');
            }
            buf.append(value);
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f - %.2f: %s", start, end, rawText);
    }
}
