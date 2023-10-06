package jkara.sync;

import java.util.List;

public record WordLine(
    double start,
    double end,
    List<TimedWord> words
)
{}
