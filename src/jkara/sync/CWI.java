package jkara.sync;

final class CWI {

    final char ch;
    Integer segment;

    CWI(char ch, Integer segment) {
        this.ch = ch;
        this.segment = segment;
    }

    @Override
    public String toString() {
        return "'" + ch + "' " + segment;
    }
}
