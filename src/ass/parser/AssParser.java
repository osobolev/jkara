package ass.parser;

import ass.model.IAssSection;
import ass.model.ParsedAss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AssParser {

    private final List<String> lines;
    private int i = 0;

    public AssParser(List<String> lines) {
        this.lines = lines;
    }

    private IAssSection parseAnySection(ISectionParser lineParser) {
        String line0 = lines.get(i++);
        lineParser.header(line0);
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.trim().startsWith("["))
                break;
            lineParser.parseLine(line);
            i++;
        }
        return lineParser.build();
    }

    public ParsedAss parse() {
        List<String> header = new ArrayList<>();
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.trim().startsWith("["))
                break;
            header.add(line);
            i++;
        }

        List<IAssSection> sections = new ArrayList<>();
        while (i < lines.size()) {
            String line = lines.get(i);
            String sectionName = line.trim();
            ISectionParser lineParser;
            switch (sectionName) {
            case "[V4+ Styles]" -> lineParser = new StyleSectionParser();
            case "[Events]" -> lineParser = new DialogSectionParser();
            default -> lineParser = new OpaqueSectionParser();
            }
            IAssSection section = parseAnySection(lineParser);
            sections.add(section);
        }
        return new ParsedAss(header, sections);
    }

    public static ParsedAss parse(Path assPath) throws IOException {
        List<String> lines = Files.readAllLines(assPath);
        return new AssParser(lines).parse();
    }
}
