package ass.parser;

import ass.model.IAssSection;
import ass.model.OpaqueSection;

import java.util.ArrayList;
import java.util.List;

final class OpaqueSectionParser extends ISectionParser {

    private String name = null;
    private final List<String> lines = new ArrayList<>();

    @Override
    void header(String line) {
        this.name = line;
        lines.add(line);
    }

    @Override
    void parseLine(String line) {
        lines.add(line);
    }

    @Override
    IAssSection build() {
        return new OpaqueSection(name, lines);
    }
}
