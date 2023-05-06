package jkara.scroll;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ParsedAss {

    final List<String> header;
    final List<AssLine> lines;

    private ParsedAss(List<String> header, List<AssLine> lines) {
        this.header = header;
        this.lines = lines;
    }

    static ParsedAss parse(Path assPath) throws IOException {
        List<String> lines = Files.readAllLines(assPath);
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i++).trim();
            if (line.startsWith("[Events]"))
                break;
        }
        while (i < lines.size()) {
            String line = lines.get(i++).trim();
            if (line.startsWith("Dialogue"))
                break;
            i++;
        }
        int firstEvent = i;
        List<AssLine> assLines = new ArrayList<>();
        while (i < lines.size()) {
            String line = lines.get(i++);
            String trimmed = line.trim();
            if (trimmed.startsWith("Dialogue")) {
                AssLine assLine = new AssLineParser(line).parse();
                assLines.add(assLine);
            }
            i++;
        }
        return new ParsedAss(lines.subList(0, firstEvent), assLines);
    }

    void write(PrintWriter pw) {
        for (String line : header) {
            pw.println(line);
        }
        for (AssLine line : lines) {
            pw.println(line.formatAss());
        }
    }
}
