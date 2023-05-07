package jkara.scroll;

import ass.model.ParsedAss;
import ass.parser.AssParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

public final class AssParserTest {

    public static void main(String[] args) throws IOException {
        Path assPath = Path.of("C:\\home\\projects\\my\\jkara\\work\\war\\subs_edited.ass");
        ParsedAss parsed = AssParser.parse(assPath);
        PrintWriter pw = new PrintWriter(System.out);
        parsed.write(pw);
        pw.flush();
    }
}
