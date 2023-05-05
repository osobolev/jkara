package jkara.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class RealText {

    final String text;
    final List<CWI> list;

    private RealText(String text, List<CWI> list) {
        this.text = text;
        this.list = list;
    }

    static RealText read(Path file) throws IOException {
        String str;
        try (BufferedReader rdr = Files.newBufferedReader(file)) {
            str = rdr.lines().collect(Collectors.joining("\n"));
        }
        List<CWI> buf = new ArrayList<>();
        Normalizer.append(buf, str, j -> j);
        Normalizer.finish(buf);
        return new RealText(str, buf);
    }
}
