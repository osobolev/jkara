package jkara.ass;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

final class RawText {

    private final String text;
    final Timestamps[] timestamps;

    private RawText(String text) {
        this.text = text;
        this.timestamps = new Timestamps[text.length()];
    }

    int size() {
        return text.length();
    }

    char get(int i) {
        return text.charAt(i);
    }

    static RawText read(Path file) throws IOException {
        String str;
        try (BufferedReader rdr = Files.newBufferedReader(file)) {
            str = rdr.lines().collect(Collectors.joining("\n"));
        }
        return new RawText(str);
    }
}
