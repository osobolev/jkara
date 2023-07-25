package jkara.setup;

import java.io.IOException;
import java.nio.file.Path;

public final class Tools {

    public final Path pythonDir;
    public final Path pythonExeDir;
    public final Path ffmpegDir;

    public Tools(Path pythonDir, Path ffmpegDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonDir == null ? null : pythonDir.resolve("Scripts");
        this.ffmpegDir = ffmpegDir;
    }

    public static Tools setup() throws IOException, InterruptedException {
        Path toolDir = Path.of(System.getProperty("user.home")).resolve(".jkara");
        return new Setup(toolDir).setup();
    }
}
