package jkara.pipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

final class StageValue<O> extends StageInput {

    private final List<Path> paths;
    private final O value;

    StageValue(List<Path> paths, O value) {
        this.paths = paths;
        this.value = value;
    }

    protected Modification getModification() {
        Path maxLastModified = null;
        FileTime maxModifiedTime = null;
        for (Path path : paths) {
            try {
                FileTime lastModified = Files.getLastModifiedTime(path);
                if (maxModifiedTime == null || lastModified.compareTo(maxModifiedTime) > 0) {
                    maxLastModified = path;
                    maxModifiedTime = lastModified;
                }
            } catch (IOException ex) {
                return modified("cannot get modification time of '%s'", path);
            }
        }
        return new NotModified(maxLastModified, maxModifiedTime);
    }

    O value() {
        return value;
    }
}
