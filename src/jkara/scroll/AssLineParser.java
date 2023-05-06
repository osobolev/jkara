package jkara.scroll;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AssLineParser {

    private final String line;
    private int i = 0;

    AssLineParser(String line) {
        this.line = line;
    }

    private void skipSpaces() {
        while (i < line.length()) {
            char ch = line.charAt(i);
            if (ch > ' ')
                break;
            i++;
        }
    }

    private String skipUntilComma() {
        skipSpaces();
        int i0 = i;
        while (i < line.length()) {
            char ch = line.charAt(i++);
            if (ch == ',') {
                return line.substring(i0, i - 1).trim();
            }
        }
        return line.substring(i0).trim();
    }

    private void skip(String str) {
        if (line.regionMatches(true, i, str, 0, str.length())) {
            i += str.length();
        }
    }

    private static final Pattern TS_FORMAT = Pattern.compile("(\\d+):(\\d+):(\\d+\\.\\d+)");

    private static double parseTimestamp(String str) {
        Matcher matcher = TS_FORMAT.matcher(str);
        if (!matcher.matches())
            throw new IllegalStateException("Wrong timestamp format: " + str);
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        double second = Double.parseDouble(matcher.group(3));
        return (hour * 60.0 + minute) * 60.0 + second;
    }

    AssLine parse() {
        skipSpaces();
        skip("Dialogue");
        skipSpaces();
        skip(":");
        String[] fields = new String[9];
        for (int j = 0; j < 9; j++) {
            fields[j] = skipUntilComma();
        }
        skipSpaces();
        String text = line.substring(i);
        double start = parseTimestamp(fields[1]);
        double end = parseTimestamp(fields[2]);
        return AssLine.create(fields, start, end, text);
    }
}
