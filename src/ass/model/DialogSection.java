package ass.model;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

public final class DialogSection implements IAssSection {

    private final String name;
    private final SectionFormat format;
    public final List<DialogLine> lines;

    public DialogSection(String name, SectionFormat format, List<DialogLine> lines) {
        this.name = name;
        this.format = format;
        this.lines = lines;
    }

    public static DialogSection empty() {
        return new DialogSection(null, null, Collections.emptyList());
    }

    DialogSection withLines(List<DialogLine> newLines) {
        return new DialogSection(name, format, newLines);
    }

    @Override
    public void write(PrintWriter pw) {
        if (name == null)
            return;
        pw.println(name);
        if (format != null) {
            pw.println("Format: " + String.join(", ", format.fields()));
            for (DialogLine line : lines) {
                pw.println(line.formatAss(format));
            }
        }
        pw.println();
    }
}
