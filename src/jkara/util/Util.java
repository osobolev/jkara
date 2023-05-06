package jkara.util;

import java.util.Locale;

public final class Util {

    public static boolean isLetter(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '\'';
    }

    public static String formatTimestamp(double ts) {
        long totalSecs = (long) ts;
        double secs = totalSecs % 60 + (ts - totalSecs);
        long totalMins = totalSecs / 60;
        long mins = totalMins % 60;
        long hours = totalMins / 60;
        return String.format(Locale.ROOT, "%s:%02d:%05.2f", hours, mins, secs);
    }
}
