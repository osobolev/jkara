package ass.model;

import java.io.PrintWriter;
import java.util.List;

public final class OpaqueSection implements IAssSection {

    public final String name;
    private final List<String> lines;

    public OpaqueSection(String name, List<String> lines) {
        this.name = name;
        this.lines = lines;
    }

    @Override
    public String toString() {
        return name == null ? "<header>" : "Section " + name;
    }

    @Override
    public void write(PrintWriter pw) {
        for (String line : lines) {
            pw.println(line);
        }
    }
}
