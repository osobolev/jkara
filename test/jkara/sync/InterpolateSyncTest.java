package jkara.sync;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class InterpolateSyncTest {

    public static void main(String[] args) throws IOException, SyncException {
        Path real = Path.of("C:\\home\\projects\\my\\jkara\\work\\test\\srb\\text.txt");
        Path fast = Path.of("C:\\home\\projects\\my\\jkara\\work\\test\\srb\\fast.json");

        List<List<TimedWord>> lines = InterpolateTextSync.sync(real, fast);
        for (List<TimedWord> line : lines) {
            System.out.println(line.stream().map(w -> w.word() + w.extra()).collect(Collectors.joining()));
        }
    }
}
