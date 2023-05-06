package jkara.scroll;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

public final class AssParserTest {

    public static void main(String[] args) throws IOException {
        Path assPath = Path.of("C:\\home\\projects\\my\\jkara\\work\\selfmachine\\subs.ass");
        ParsedAss parsed = ParsedAss.parse(assPath);
        PrintWriter pw = new PrintWriter(System.out);
        parsed.write(pw);
        pw.flush();
    }
}
