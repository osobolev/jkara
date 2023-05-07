package ass.model;

import java.io.PrintWriter;
import java.util.List;

public final class OpaqueSection extends IAssSection {

    private final List<String> lines;

    public OpaqueSection(List<String> lines) {
        this.lines = lines;
    }

    @Override
    public void write(PrintWriter pw) {
        for (String line : lines) {
            pw.println(line);
        }
    }
}
