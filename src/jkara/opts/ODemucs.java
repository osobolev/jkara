package jkara.opts;

public record ODemucs(
    int shifts
) {

    public ODemucs() {
        this(1);
    }
}
