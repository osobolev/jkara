package jkara.scroll;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AssParserTest {

    public static void main(String[] args) throws IOException {
        Path assPath = Path.of("C:\\home\\projects\\my\\jkara\\work\\selfmachine\\subs.ass");
        ParsedAss parsed = ParsedAss.parse(assPath);
        List<List<AssLine>> groups = AssJoiner.splitByPauses(parsed.lines, 2.5);
        List<List<AssLine>> allPortions = new ArrayList<>();
        for (List<AssLine> group : groups) {
            List<List<AssLine>> portions = AssJoiner.splitByCount(group, 4);
            allPortions.addAll(portions);
        }
        List<AssLine> newLines = new ArrayList<>();
        for (List<AssLine> group : allPortions) {
            AssLine newLine = AssJoiner.join(group);
            newLines.add(newLine);
        }
        ParsedAss newAss = new ParsedAss(parsed.header, newLines);
        try (PrintWriter pw = new PrintWriter("C:\\home\\projects\\my\\jkara\\work\\selfmachine\\new_subs.ass", StandardCharsets.UTF_8)) {
            newAss.write(pw);
        }
    }
}
