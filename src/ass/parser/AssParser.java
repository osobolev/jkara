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

    private static boolean isSectionStart(String line) {
        return line.trim().startsWith("[");
    }

    private IAssSection parseSection(ISectionParser lineParser) {
        String line0 = line();
        i++;
        lineParser.header(line0);
        while (!eof()) {
            String line = line();
            if (isSectionStart(line))
                break;
            lineParser.parseLine(line);
            i++;
        }
        return lineParser.build();
    }

    public ParsedAss parse() {
        List<String> header = new ArrayList<>();
        while (!eof()) {
            String line = line();
            if (isSectionStart(line))
                break;
            header.add(line);
            i++;
        }

        List<IAssSection> sections = new ArrayList<>();
        while (!eof()) {
            String line = line();
            String sectionName = line.trim();
            ISectionParser lineParser;
            switch (sectionName) {
            case "[V4+ Styles]" -> lineParser = new StyleSectionParser();
            case "[Events]" -> lineParser = new DialogSectionParser();
            default -> lineParser = new OpaqueSectionParser();
            }
            IAssSection section = parseSection(lineParser);
            sections.add(section);
        }
        return new ParsedAss(header, sections);
    }

    public static ParsedAss parse(Path assPath) throws IOException {
        List<String> lines = Files.readAllLines(assPath);
        return new AssParser(lines).parse();
    }
}
