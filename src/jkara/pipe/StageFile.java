package jkara.pipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

final class StageFile {

    private final Path path;
    private final boolean optional;
    private final StageFile[] dependsOn;
    private boolean modified = false;

    StageFile(Path path, boolean optional, StageFile... dependsOn) {
        this.path = path;
        this.optional = optional;
        this.dependsOn = dependsOn;
    }

    private sealed interface Modification permits Modified, NotModified {
    }

    private record Modified(String cause) implements Modification {}

    private record NotModified(FileTime lastModified) implements Modification {}

    /**
     * Is file modified relative to its dependencies
     */
    String isModified() {
        Modification modification = getModification();
        if (modification instanceof Modified mod) {
            return mod.cause;
        } else {
            return null;
        }
    }

    private static Modified modified(String format, Path... paths) {
        return new Modified(String.format(format, (Object[]) paths));
    }

    private Modification getModification() {
        if (modified)
            return modified("file '%s' was rebuilt", path.getFileName());
        if (!Files.exists(path)) {
            if (optional) {
                return new NotModified(null);
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
        for (StageFile dep : dependsOn) {
            Modification depModification = dep.getModification();
            if (depModification instanceof Modified) {
                return depModification;
            } else if (depModification instanceof NotModified depFile) {
                if (depFile.lastModified() != null && depFile.lastModified().compareTo(lastModified) > 0)
                    return modified("file '%s' is newer than '%s'", dep.path.getFileName(), path.getFileName());
            }
        }
        return new NotModified(lastModified);
    }

    Path input() {
        return path;
    }

    Path output() {
        modified = true;
        return path;
    }
}
