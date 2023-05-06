package jkara.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class RealText {

    final List<CWS> list;

    private RealText(List<CWS> list) {
        this.list = list;
    }

    static RealText read(Path file) throws IOException {
        String str;
        try (BufferedReader rdr = Files.newBufferedReader(file)) {
            str = rdr.lines().collect(Collectors.joining("\n"));
        }
        List<CWS> buf = new ArrayList<>();
        Normalizer.append(buf, str, null);
        Normalizer.finish(buf);
        return new RealText(buf);
    }
}
