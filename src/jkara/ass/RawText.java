package jkara.ass;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

final class RawText {

    final CSegment[] chars;

    private RawText(CSegment[] chars) {
        this.chars = chars;
    }

    int size() {
        return chars.length;
    }

    char get(int i) {
        return chars[i].ch;
    }

    static RawText read(Path file) throws IOException {
        String str;
        try (BufferedReader rdr = Files.newBufferedReader(file)) {
            str = rdr.lines().collect(Collectors.joining("\n"));
        }
        CSegment[] chars = new CSegment[str.length()];
        for (int i = 0; i < str.length(); i++) {
            chars[i] = new CSegment(str.charAt(i), null);
        }
        return new RawText(chars);
    }
}
