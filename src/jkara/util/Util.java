package jkara.util;

public final class Util {

    public static boolean isLetter(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '\'';
    }

    public static void appendK(StringBuilder buf, double len) {
        long k = Math.round(len * 100);
        if (k <= 0)
            return;
        buf.append(String.format("{\\k%s}", k));
    }
}
