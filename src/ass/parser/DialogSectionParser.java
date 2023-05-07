package ass.parser;

import ass.model.DialogLine;
import ass.model.DialogSection;
import ass.model.IAssSection;
import ass.model.SectionFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class DialogSectionParser extends FormattedSectionParser {

    private String name = null;
    private final List<DialogLine> lines = new ArrayList<>();

    @Override
    void header(String line) {
        this.name = line;
    }

    @Override
    protected void parseLine(SectionFormat format, String line) {
        Map<String, String> map = new LineParser(line).parseLine("Dialogue", format);
        if (map != null) {
            lines.add(DialogLine.create(map));
        }
    }

    @Override
    IAssSection build() {
        return new DialogSection(name, format, lines);
    }
}
