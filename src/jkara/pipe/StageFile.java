package jkara.pipe;

import java.nio.file.Files;
import java.nio.file.Path;

final class StageFile {

    private final Path path;
    private final StageFile[] dependsOn;
    private boolean modified = false;

    StageFile(Path path, StageFile... dependsOn) {
        this.path = path;
        this.dependsOn = dependsOn;
    }

    boolean isModified() {
        if (modified)
            return true;
        if (!Files.exists(path))
            return true;
        for (StageFile dep : dependsOn) {
            if (dep.isModified())
                return true;
        }
        return false;
    }

    Path input() {
        return path;
    }

    Path output() {
        modified = true;
        return path;
    }
}
