package ass.model;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class StyleSection implements IAssSection {

    private final List<String> lines;
    public final Map<String, AssStyle> styles;

    public StyleSection(List<String> lines, Map<String, AssStyle> styles) {
        this.lines = lines;
        this.styles = styles;
    }

    public static StyleSection empty() {
        return new StyleSection(Collections.emptyList(), Collections.emptyMap());
    }

    @Override
    public void write(PrintWriter pw) {
        for (String line : lines) {
            pw.println(line);
        }
    }
}
