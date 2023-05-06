package jkara.scroll;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record AssLine(
    String[] fields,
    double start,
    double end,
    String text,
    String rawText,
    double sumLen
) {

    private static final Pattern K_TAG = Pattern.compile("\\\\\\s*k[a-z]*\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    static AssLine create(String[] fields, double start, double end, String text) {
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
        return new AssLine(fields, start, end, text, rawText, sumLen / 100.0);
    }

    String formatAss() {
        StringBuilder buf = new StringBuilder("Dialogue: ");
        for (String field : fields) {
            buf.append(field).append(',');
        }
        buf.append(text);
        return buf.toString();
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f - %.2f: %s", start, end, rawText);
    }
}
