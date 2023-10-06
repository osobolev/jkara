package jkara.sync;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class WordSyncTest {

    public static void main(String[] args) throws IOException, SyncException {
        Path real = Path.of("C:\\home\\projects\\my\\jkara\\work\\test\\srb\\text.txt");
        Path fast = Path.of("C:\\home\\projects\\my\\jkara\\work\\test\\srb\\fast.json");

        List<WordLine> lines = WordTextSync.sync(real, fast);
        for (WordLine line : lines) {
            System.out.println(line.words().stream().map(TimedWord::text).collect(Collectors.joining()));
        }
    }
}
