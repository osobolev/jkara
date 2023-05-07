package ass.model;

import java.util.Map;

public record AssStyle(
    Map<AssStyleKey, String> values
)
{}
