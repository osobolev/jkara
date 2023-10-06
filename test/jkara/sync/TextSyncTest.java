package jkara.sync;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

public final class TextSyncTest {

    public static void main(String[] args) throws IOException, SyncException {
//        Path real = Path.of("C:\\home\\projects\\my\\kara\\work\\text.txt");
//        Path fast = Path.of("C:\\home\\projects\\my\\kara\\work\\_fast.json");
//        Path real = Path.of("C:\\Downloads\\kara\\war\\text.txt");
//        Path fast = Path.of("C:\\Downloads\\kara\\war\\fast.json");
        Path real = Path.of("C:\\home\\projects\\my\\jkara\\work\\test\\srb\\text.txt");
        Path fast = Path.of("C:\\home\\projects\\my\\jkara\\work\\test\\srb\\fast.json");

        StringWriter sw = new StringWriter();
        TextSync.sync(real, fast, () -> sw);
        System.out.println(sw);
    }
}
