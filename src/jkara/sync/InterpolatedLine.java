package jkara.sync;

import java.util.List;

public record InterpolatedLine(
    double start,
    double end,
    List<TimedWord> words
)
{}
