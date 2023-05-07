package jkara.pipe;

import java.nio.file.Path;

final class Stages {

    private final Path workDir;

    Stages(Path workDir) {
        this.workDir = workDir;
    }

    StageFile file(String name, StageFile... dependsOn) {
        Path path = workDir.resolve(name);
        return new StageFile(path, false, dependsOn);
    }
}
