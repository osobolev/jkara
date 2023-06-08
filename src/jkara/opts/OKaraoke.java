package jkara.opts;

public record OKaraoke(
    double betweenGroups,
    double shift,

    double preview1,
    double preview,
    double previewAfterSolo,
    double minSoloLength,

    double minTitles,
    double maxTitles,
    double minAfterTitles,

    boolean video
) {

    public OKaraoke() {
        this(
            2.5, 0.1,

            2.0, 0.75, 0.75, 20.0,

            1.0, 5.0, 1.0,

            true
        );
    }
}
