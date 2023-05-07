package ass.model;

import java.util.Map;

public record AssStyle(
    Map<AssStyleProperty, String> values
)
{}
