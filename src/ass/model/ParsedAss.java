package ass.model;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public final class ParsedAss {

    private final List<String> header;
    public final List<IAssSection> sections;
    public final StyleSection styles;
    public final DialogSection dialog;

    public ParsedAss(List<String> header, List<IAssSection> sections) {
        this.header = header;
        this.sections = sections;
        StyleSection styles = null;
        DialogSection dialog = null;
        for (IAssSection section : sections) {
            if (section instanceof StyleSection found) {
                styles = found;
            } else if (section instanceof DialogSection found) {
                dialog = found;
            }
        }
        this.styles = styles == null ? StyleSection.empty() : styles;
        this.dialog = dialog == null ? DialogSection.empty() : dialog;
    }

    public ParsedAss withLines(List<DialogLine> newLines) {
        List<IAssSection> newSections = new ArrayList<>();
        for (IAssSection section : sections) {
            if (section instanceof DialogSection dlg) {
                newSections.add(dlg.withLines(newLines));
            } else {
                newSections.add(section);
            }
        }
        return new ParsedAss(header, newSections);
    }

    public List<DialogLine> getLines() {
        return dialog.lines;
    }

    public void write(PrintWriter pw) {
        for (String line : header) {
            pw.println(line);
        }
        for (IAssSection section : sections) {
            section.write(pw);
        }
    }
}
