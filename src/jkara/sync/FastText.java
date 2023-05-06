package jkara.sync;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class FastText {

    public static String textFromFast(Path file) throws IOException {
        FastResult fast = FastResult.read(file);
        return fast.segments.stream().map(Segment::text).collect(Collectors.joining("\n"));
    }
}
