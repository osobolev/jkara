package ass.parser;

import ass.model.SectionFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LineParser {

    private final String line;
    private int i = 0;

    LineParser(String line) {
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

    private boolean skip(String str) {
        if (line.regionMatches(true, i, str, 0, str.length())) {
            i += str.length();
            return true;
        } else {
            return false;
        }
    }

    private boolean skipStart(String start) {
        skipSpaces();
        if (!skip(start))
            return false;
        skipSpaces();
        skip(":");
        return true;
    }

    private String getRest() {
        return line.substring(i);
    }

    SectionFormat parseFormat() {
        skipStart("Format");
        List<String> fields = new ArrayList<>();
        while (true) {
            String name = skipUntilComma().trim();
            if (name.isEmpty())
                break;
            fields.add(name);
        }
        return new SectionFormat(fields);
    }

    Map<String, String> parseLine(String start, SectionFormat format) {
        if (!skipStart(start))
            return null;
        Map<String, String> values = new HashMap<>();
        List<String> fields = format.fields();
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            String value;
            if (i == fields.size() - 1) {
                skipSpaces();
                value = getRest();
            } else {
                value = skipUntilComma();
            }
            values.put(field, value);
        }
        return values;
    }
}
