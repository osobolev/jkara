package jkara.ass;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

public final class AssSyncTest {

    public static void main(String[] args) throws IOException {
//        Path text = Path.of("C:\\home\\projects\\my\\kara\\work\\text.txt");
//        Path aligned = Path.of("C:\\home\\projects\\my\\kara\\work\\aligned.json");
//        Path text = Path.of("C:\\Downloads\\kara\\war\\text.txt");
//        Path aligned = Path.of("C:\\Downloads\\kara\\war\\aligned.json");
        Path text = Path.of("C:\\home\\projects\\my\\jkara\\work\\rammstein\\text.txt");
        Path aligned = Path.of("C:\\home\\projects\\my\\jkara\\work\\rammstein\\aligned.json");

        StringWriter sw = new StringWriter();
        AssSync.sync(text, text, aligned, () -> sw);
        System.out.println(sw);
    }
}
