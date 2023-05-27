package jkara.pipe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

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
    private void runCommand(String what, List<String> args, Consumer<Process> out, IntPredicate exitOk) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(args);
        if (ffmpegDir != null) {
            String path = pb.environment().get("PATH");
            pb.environment().put("PATH", path == null ? ffmpegDir.toString() : ffmpegDir + File.pathSeparator + path);
        }
        Process p = pb.start();
        capture(p.getErrorStream(), System.err);
        if (out != null) {
            out.accept(p);
        } else {
            capture(p.getInputStream(), System.out);
        }
        int exitCode = p.waitFor();
        boolean ok;
        if (exitOk != null) {
            ok = exitOk.test(exitCode);
        } else {
            ok = exitCode == 0;
        }
        if (!ok)
            throw new IOException("Error running " + what + ": " + exitCode);
    }

    void runPython(String script, String... args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add("python");
        list.add(rootDir.resolve(script).toString());
        list.addAll(Arrays.asList(args));
        runCommand("script " + script, list, null, null);
    }

    void runExe(String exe, List<String> args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add(exe);
        list.addAll(args);
        runCommand(exe, list, null, null);
    }

    void runExe(String exe, String... args) throws IOException, InterruptedException {
        runExe(exe, Arrays.asList(args));
    }

    private void runFF(String ff, List<String> args, Consumer<Process> out, IntPredicate exitOk) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        String ffPath = ffmpegDir == null ? ff : ffmpegDir.resolve(ff).toString();
        list.add(ffPath);
        list.addAll(List.of("-v", "quiet"));
        list.addAll(args);
        runCommand(ff, list, out, exitOk);
    }

    void runFFMPEG(List<String> args) throws IOException, InterruptedException {
        runFF("ffmpeg", args, null, null);
    }

    void runFFProbe(List<String> args, Consumer<Process> out, IntPredicate exitOk) throws IOException, InterruptedException {
        runFF("ffprobe", args, out, exitOk);
    }
}
