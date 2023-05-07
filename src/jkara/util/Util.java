package jkara.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Util {

    public static boolean isLetter(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '\'';
    }

    public static void appendK(StringBuilder buf, String tag, double len) {
        long k = Math.round(len * 100);
        if (k <= 0)
            return;
        buf.append(String.format("{\\%s%s}", tag, k));
    }

    public static void appendK(StringBuilder buf, double len) {
        appendK(buf, "k", len);
    }

    public static String readLyrics(Path file) throws IOException {
        try (BufferedReader rdr = Files.newBufferedReader(file)) {
            Predicate<String> normalLine = Pattern.compile("\\[.*]").asMatchPredicate().negate();
            return rdr
                .lines()
                .map(String::trim)
                .filter(normalLine)
                .collect(Collectors.joining("\n"));
        }
    }
}
