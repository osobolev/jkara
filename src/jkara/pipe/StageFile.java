package jkara.pipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

final class StageFile extends StageInput {

    private final Path path;
    private final boolean optional;
    private final StageInput[] dependsOn;
    private boolean modified = false;

    StageFile(Path path, boolean optional, StageInput... dependsOn) {
        this.path = path;
        this.optional = optional;
        this.dependsOn = dependsOn;
    }

    StageFile(Path path, StageInput... dependsOn) {
        this(path, false, dependsOn);
    }

    protected Modification getModification() {
        if (modified)
            return modified("file '%s' was rebuilt", path.getFileName());
        if (!Files.exists(path)) {
            if (optional) {
                return new NotModified(null, null);
            } else {
                return modified("file '%s' does not exist", path.getFileName());
            }
        }
        FileTime lastModified;
        try {
            lastModified = Files.getLastModifiedTime(path);
        } catch (IOException ex) {
            return modified("cannot get modification time of '%s'", path);
        }
        for (StageInput dep : dependsOn) {
            Modification depModification = dep.getModification();
            if (depModification instanceof Modified) {
                return depModification;
            } else if (depModification instanceof NotModified depFile) {
                if (depFile.lastModified() != null && depFile.lastModified().compareTo(lastModified) > 0)
                    return modified("file '%s' is newer than '%s'", depFile.file().getFileName(), path.getFileName());
            }
        }
        return new NotModified(path, lastModified);
    }

    Path input() {
        return path;
    }

    Path output() {
        modified = true;
        return path;
    }
}
