package jkara.scroll;

import ass.model.DialogLine;
import ass.model.ParsedAss;
import ass.parser.AssParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AssParserTest {

    public static void main(String[] args) throws IOException {
        Path assPath = Path.of("C:\\home\\projects\\my\\jkara\\work\\war\\subs_edited.ass");
        ParsedAss parsed = AssParser.parse(assPath);
        List<List<DialogLine>> groups = AssJoiner.splitByPauses(parsed.getLines(), 2.5);
        List<List<DialogLine>> allPortions = new ArrayList<>();
        for (List<DialogLine> group : groups) {
            List<List<DialogLine>> portions = AssJoiner.splitByCount(group, 4);
            allPortions.addAll(portions);
        }
        List<DialogLine> newLines = new ArrayList<>();
        for (List<DialogLine> group : allPortions) {
//            AssLine newLine = AssJoiner.join(group);
//            newLines.add(newLine);
            for (int i = 0; i < group.size(); i++) {
                DialogLine fakeLine = AssJoiner.join2(group, i);
                newLines.add(fakeLine);
            }
        }
        ParsedAss newAss = parsed.withLines(newLines);
        try (PrintWriter pw = new PrintWriter("C:\\home\\projects\\my\\jkara\\work\\war\\new_subs.ass", StandardCharsets.UTF_8)) {
            newAss.write(pw);
        }
    }
}
