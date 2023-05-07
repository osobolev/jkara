package ass.parser;

import ass.model.SectionFormat;

abstract class FormattedSectionParser extends ISectionParser {

    protected SectionFormat format = null;

    @Override
    void parseLine(String line) {
        if (line.trim().startsWith("Format")) {
            format = new LineParser(line).parseFormat();
        } else if (format != null) {
            parseLine(format, line);
        }
    }

    protected abstract void parseLine(SectionFormat format, String line);
}
