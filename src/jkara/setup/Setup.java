package jkara.setup;

import jkara.util.ProcUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Setup {

    private static final String PYTHON_URL = "https://www.python.org/ftp/python/3.10.11/python-3.10.11-embed-amd64.zip";
    private static final String GET_PIP_URL = "https://bootstrap.pypa.io/get-pip.py";
    private static final String WHISPERX_URL = "https://github.com/osobolev/whisperX/archive/refs/heads/main.zip";
    private static final String FFMPEG_URL = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";

    private final Path pythonDir;
    private final Path ffmpegDir;

    Setup(Path toolDir) {
        this.pythonDir = toolDir.resolve("python");
        this.ffmpegDir = toolDir.resolve("ffmpeg");
    }

    private interface ContentHandler {

        void accept(InputStream is) throws IOException;
    }

    private static void download(String url, ContentHandler handler) throws IOException {
        URLConnection conn = new URL(url).openConnection();
        try (InputStream is = conn.getInputStream()) {
            handler.accept(is);
        }
        if (conn instanceof HttpURLConnection http) {
            http.disconnect();
        }
    }

    private static void downloadZip(String url, Function<String, Path> getDest) throws IOException {
        download(url, is -> {
            try (ZipInputStream zis = new ZipInputStream(is)) {
                while (true) {
                    ZipEntry e = zis.getNextEntry();
                    if (e == null)
                        break;
                    if (e.isDirectory())
                        continue;
                    Path dest = getDest.apply(e.getName());
                    if (dest == null)
                        continue;
                    Files.createDirectories(dest.getParent());
                    Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        });
    }

    private void runPython(String what, Path exe, String... args) throws IOException, InterruptedException {
        List<Path> pathDirs = List.of(pythonDir, pythonDir.resolve("Scripts"));
        ProcUtil.runCommand(what, exe, List.of(args), pathDirs, null, null);
    }

    private void installPython() throws IOException {
        downloadZip(PYTHON_URL, name -> {
            if (name.endsWith("._pth")) {
                return null;
            } else {
                return pythonDir.resolve(name);
            }
        });
    }

    private void installPIP() throws IOException, InterruptedException {
        Path getPip = pythonDir.resolve("get-pip.py");
        download(GET_PIP_URL, is -> Files.copy(is, getPip, StandardCopyOption.REPLACE_EXISTING));

        runPython(
            "get-pip",
            pythonDir.resolve("python"), getPip.toString()
        );
    }

    private void installPackages() throws IOException, InterruptedException {
        Path pipExe = pythonDir.resolve(Path.of("Scripts", "pip"));
        runPython(
            "pip",
            pipExe, "-v", "install", "yt-dlp", "PySoundFile", "demucs", "faster_whisper", WHISPERX_URL
        );
    }

    private void setupPython() throws IOException, InterruptedException {
        installPython();
        installPIP();
        installPackages();
    }

    private void installFFMPEG() throws IOException {
        downloadZip(FFMPEG_URL, name -> {
            Path sub = Path.of(name);
            int len = sub.getNameCount();
            if (len <= 1)
                return null;
            return ffmpegDir.resolve(sub.subpath(1, len));
        });
    }

    private interface Step {

        void run() throws IOException, InterruptedException;
    }

    private static void runStep(Path folder, Step step) throws IOException, InterruptedException {
        Path done = folder.resolve(".done");
        if (Files.exists(done))
            return;
        step.run();
        Files.write(done, new byte[0]);
    }

    Tools setup() throws IOException, InterruptedException {
        runStep(pythonDir, this::setupPython);
        runStep(ffmpegDir, this::installFFMPEG);
        return new Tools(pythonDir, ffmpegDir.resolve("bin"));
    }
}
