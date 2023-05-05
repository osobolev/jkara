package jkara;

public final class Util {

    public static boolean isLetter(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '\'';
    }
}
