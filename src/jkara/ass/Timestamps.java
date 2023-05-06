package jkara.ass;

record Timestamps(
    double start,
    double end
) {

    static Timestamps create(double start, double end) {
        if (Double.isNaN(start) || Double.isNaN(end))
            return null;
        return new Timestamps(start, end);
    }
}
