package jkara.setup;

import jkara.opts.OptFile;

import java.io.IOException;
import java.nio.file.Path;

record SoftSources(
    String pythonUrl,
    String getPipUrl,
    String whisperxUrl,
    String ffmpegUrl
) {

    SoftSources() {
        this(
            "https://www.python.org/ftp/python/3.10.11/python-3.10.11-embed-amd64.zip",
            "https://bootstrap.pypa.io/get-pip.py",
            "https://github.com/osobolev/whisperX/archive/refs/heads/main.zip",
            "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
        );
    }

    static SoftSources create(Path rootDir) throws IOException {
        Path sources = rootDir.resolve("soft.properties");
        return OptFile.read(SoftSources.class, sources).value;
    }
}
