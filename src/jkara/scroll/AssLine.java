package jkara.scroll;

import java.util.Locale;

record AssLine(
    String[] fields,
    double start,
    double end,
    String text
) {

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
        return String.format(Locale.ROOT, "%.2f - %.2f: %s", start, end, text);
    }
}
