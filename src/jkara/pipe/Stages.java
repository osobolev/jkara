package jkara.pipe;

import jkara.opts.OptFile;

import java.io.IOException;
import java.nio.file.Path;

final class Stages {

    private final Path rootDir;
    private final Path workDir;

    Stages(Path rootDir, Path workDir) {
        this.rootDir = rootDir;
        this.workDir = workDir;
    }

    StageFile file(String name, StageInput... dependsOn) {
        Path path = workDir.resolve(name);
        return new StageFile(path, dependsOn);
    }

    <O extends Record> StageValue<O> options(String name, Class<O> cls) throws IOException {
        OptFile<O> of = OptFile.read(rootDir, workDir, name, cls);
        return new StageValue<>(of.files, of.value);
    }
}
