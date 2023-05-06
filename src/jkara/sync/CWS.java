package jkara.sync;

final class CWS {

    final char ch;
    Integer segment;

    CWS(char ch, Integer segment) {
        this.ch = ch;
        this.segment = segment;
    }

    @Override
    public String toString() {
        return "'" + ch + "' " + segment;
    }
}
