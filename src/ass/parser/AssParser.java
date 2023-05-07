package ass.parser;

import ass.model.IAssSection;
import ass.model.OpaqueSection;
import ass.model.ParsedAss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class AssParser {

    private final List<String> lines;
    private int i = 0;

    public AssParser(List<String> lines) {
        this.lines = lines;
    }

    private boolean eof() {
        return i >= lines.size();
    }

    private String line() {
        String line = lines.get(i);
        if (i == 0 && line.startsWith("\uFEFF")) {
            return line.substring(1);
        } else {
            return line;
        }
    }

    private void readUntilNextSection(Consumer<String> consumer) {
        while (!eof()) {
            String line = line();
            if (line.trim().startsWith("["))
                break;
            consumer.accept(line);
            i++;
        }
    }

    private IAssSection parseSection(ISectionParser lineParser) {
        String line0 = line();
        i++;
        lineParser.header(line0);
        readUntilNextSection(lineParser::parseLine);
        return lineParser.build();
    }

    public ParsedAss parse() {
        List<String> header = new ArrayList<>();
        readUntilNextSection(header::add);

        List<IAssSection> sections = new ArrayList<>();
        if (!header.isEmpty()) {
            sections.add(new OpaqueSection(null, header));
        }
        while (!eof()) {
            String line = line();
            String sectionName = line.trim();
            ISectionParser lineParser = switch (sectionName) {
                case "[V4+ Styles]" -> new StyleSectionParser();
                case "[Events]" -> new DialogSectionParser();
                default -> new OpaqueSectionParser();
            };
            IAssSection section = parseSection(lineParser);
            sections.add(section);
        }
        return new ParsedAss(sections);
    }

    public static ParsedAss parse(Path assPath) throws IOException {
        List<String> lines = Files.readAllLines(assPath);
        return new AssParser(lines).parse();
    }
}
