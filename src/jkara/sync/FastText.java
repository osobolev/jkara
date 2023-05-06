package jkara.sync;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FastText {

    public static void textFromFast(Path fastJson, Path text) throws IOException {
        FastResult fast = FastResult.read(fastJson);
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(text))) {
            for (Segment segment : fast.segments) {
                pw.println(segment.text().trim());
            }
        }
    }
}
