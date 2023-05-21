package jkara.pipe;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

abstract class StageInput {

    protected sealed interface Modification permits Modified, NotModified {

    }

    protected record Modified(String cause) implements Modification {}

    protected record NotModified(Path file, FileTime lastModified) implements Modification {}

    /**
     * Is file modified relative to its dependencies
     */
    final String isModified() {
        Modification modification = getModification();
        if (modification instanceof Modified mod) {
            return mod.cause;
        } else {
            return null;
        }
    }

    protected static Modified modified(String format, Path... paths) {
        return new Modified(String.format(format, (Object[]) paths));
    }

    protected abstract Modification getModification();
}
