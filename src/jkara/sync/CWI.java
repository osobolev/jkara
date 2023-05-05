package jkara.sync;

record CWI(
    char ch,
    Integer index
) {

    @Override
    public String toString() {
        return "'" + ch + "' " + index;
    }
}
