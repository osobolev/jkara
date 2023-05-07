package jkara.pipe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ProcRunner {

    private final Path ffmpegDir;
    private final Path rootDir;

    ProcRunner(Path ffmpegDir, Path rootDir) {
        this.ffmpegDir = ffmpegDir;
        this.rootDir = rootDir;
    }

    private static void capture(InputStream is, OutputStream os) {
        Thread thread = new Thread(() -> {
            try {
                is.transferTo(os);
            } catch (IOException ex) {
                // ignore
            }
        });
        thread.start();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void runCommand(String what, List<String> args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(args);
        if (ffmpegDir != null) {
            String path = pb.environment().get("PATH");
            pb.environment().put("PATH", path == null ? ffmpegDir.toString() : ffmpegDir + File.pathSeparator + path);
        }
        Process p = pb.start();
        capture(p.getInputStream(), System.out);
        capture(p.getErrorStream(), System.err);
        int exitCode = p.waitFor();
        if (exitCode != 0)
            throw new IOException("Error running " + what + ": " + exitCode);
    }

    void runPython(String script, String... args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add("python");
        list.add(rootDir.resolve(script).toString());
        list.addAll(Arrays.asList(args));
        runCommand("script " + script, list);
    }

    void runExe(String exe, List<String> args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add(exe);
        list.addAll(args);
        runCommand(exe, list);
    }

    void runExe(String exe, String... args) throws IOException, InterruptedException {
        runExe(exe, Arrays.asList(args));
    }

    void runFFMPEG(List<String> args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        String ffmpeg = "ffmpeg";
        String ffmpegPath = ffmpegDir == null ? ffmpeg : ffmpegDir.resolve(ffmpeg).toString();
        list.add(ffmpegPath);
        list.addAll(args);
        runCommand("ffmpeg", list);
    }
}
