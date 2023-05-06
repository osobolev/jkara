package jkara.ass;

final class CSegment {

    final char ch;
    Timestamps timestamps;

    CSegment(char ch, Timestamps timestamps) {
        this.ch = ch;
        this.timestamps = timestamps;
    }

    @Override
    public String toString() {
        return String.valueOf(ch);
    }
}
