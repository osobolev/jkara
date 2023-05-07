package ass.parser;

import ass.model.*;

import java.util.*;

final class StyleSectionParser extends FormattedSectionParser {

    private final List<String> lines = new ArrayList<>();
    private final Map<String, AssStyle> styles = new HashMap<>();

    @Override
    void header(String line) {
        lines.add(line);
    }

    @Override
    void parseLine(String line) {
        lines.add(line);
        super.parseLine(line);
    }

    @Override
    protected void parseLine(SectionFormat format, String line) {
        Map<String, String> map = new LineParser(line).parseLine("Style", format);
        if (map != null) {
            String name = null;
            Map<AssStyleKey, String> values = new EnumMap<>(AssStyleKey.class);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String field = entry.getKey();
                String value = entry.getValue();
                if ("Name".equals(field)) {
                    name = value;
                    continue;
                }
                try {
                    AssStyleKey key = AssStyleKey.valueOf(field);
                    values.put(key, value);
                } catch (IllegalArgumentException ex) {
                    // ignore unrecognized fields
                }
            }
            if (name != null) {
                AssStyle style = new AssStyle(values);
                styles.put(name, style);
            }
        }
    }

    @Override
    IAssSection build() {
        return new StyleSection(lines, styles);
    }
}
