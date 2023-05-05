package jkara.ass;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class RawText {

    final List<String> list;

    private RawText(List<String> list) {
        this.list = list;
    }

    static RawText read(Path file) throws IOException {
        String str;
        try (BufferedReader rdr = Files.newBufferedReader(file)) {
            str = rdr.lines().collect(Collectors.joining("\n"));
        }
        List<String> buf = new ArrayList<>();
        for (int j = 0; j < str.length(); j++) {
            char ch = str.charAt(j);
            buf.add(String.valueOf(ch));
        }
        return new RawText(buf);
    }
}
