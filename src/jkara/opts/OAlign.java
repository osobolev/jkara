package jkara.opts;

public record OAlign(
    boolean interpolate
) {

    public OAlign() {
        this(false);
    }
}
