package jkara.setup;

import jkara.util.ProcUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static jkara.util.ProcUtil.log;

final class Setup {

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

    private List<Path> getPathFiles() throws IOException {
        List<Path> pathFiles = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(pythonDir)) {
            for (Path file : files) {
                if (file.getFileName().toString().endsWith("._pth")) {
                    pathFiles.add(file);
                }
            }
        }
        return pathFiles;
    }

    private static void patchPathFile(Path file) throws IOException {
        List<String> toAdd = List.of("Lib" + File.separator + "site-packages");
        List<String> lines = Files.readAllLines(file);
        Set<String> missing = new HashSet<>(toAdd);
        List<String> real = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;
            missing.remove(line);
            real.add(line);
        }
        if (missing.isEmpty())
            return;
        for (String line : toAdd) {
            if (missing.contains(line)) {
                real.add(line);
            }
        }
        Files.write(file, real);
    }

    private void installPython() throws IOException {
        log("Downloading Python...");
        downloadZip(PYTHON_URL, pythonDir::resolve);

        for (Path file : getPathFiles()) {
            patchPathFile(file);
        }
    }

    private void installPIP() throws IOException, InterruptedException {
        log("Installing PIP...");
        Path getPip = pythonDir.resolve("get-pip.py");
        download(GET_PIP_URL, is -> Files.copy(is, getPip, StandardCopyOption.REPLACE_EXISTING));

        runPython(
            "get-pip",
            pythonDir.resolve("python"), getPip.toString()
        );
    }

    private void installPackages() throws IOException, InterruptedException {
        log("Installing required packages...");
        Path pipExe = pythonDir.resolve(Path.of("Scripts", "pip"));
        runPython(
            "pip",
            pipExe, "-v", "install", "yt-dlp", "PySoundFile", "demucs", "faster_whisper"
        );
        // Неизвестно почему с файлом ._pth не устанавливается - удаляем:
        for (Path path : getPathFiles()) {
            Files.delete(path);
        }
        runPython(
            "pip",
            pipExe, "-v", "install", WHISPERX_URL
        );
    }

    private void setupPython() throws IOException, InterruptedException {
        installPython();
        installPIP();
        installPackages();
    }

    private void installFFMPEG() throws IOException {
        log("Downloading FFMPEG...");
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

    private static void runStep(String what, Path folder, Step step) throws IOException, InterruptedException {
        Path done = folder.resolve(".done");
        if (Files.exists(done))
            return;
        log("Installing " + what);
        step.run();
        Files.write(done, new byte[0]);
    }

    Tools setup() throws IOException, InterruptedException {
        runStep("Python", pythonDir, this::setupPython);
        runStep("FFMPEG", ffmpegDir, this::installFFMPEG);
        return new Tools(pythonDir, ffmpegDir.resolve("bin"));
    }
}
