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

    private final Path ffmpeg;

    ProcRunner(Path ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    void runCommand(String what, List<String> args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(args);
        String path = pb.environment().get("PATH");
        pb.environment().put("PATH", path == null ? ffmpeg.toString() : ffmpeg + File.pathSeparator + path);
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
        list.add(script);
        list.addAll(Arrays.asList(args));
        runCommand("script " + script, list);
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
}
