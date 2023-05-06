package jkara.pipe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;

final class PythonRunner {

    static void runScript(String script, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("python");
        pb.command().add(script);
        pb.command().addAll(Arrays.asList(args));
        pb.redirectErrorStream(true);
        String path = pb.environment().get("PATH");
        String ffmpeg = Path.of("ffmpeg").toAbsolutePath().toString(); // todo
        pb.environment().put("PATH", path == null ? ffmpeg : ffmpeg + File.pathSeparator + path);
        Process p = pb.start();
        // todo: charset depends on OS!!!
        Charset charset = Charset.forName("Cp1251");
        // todo: copy binary streams
        // todo: do not join out/err
        try (BufferedReader rdr = new BufferedReader(new InputStreamReader(p.getInputStream(), charset))) {
            while (true) {
                String line = rdr.readLine();
                if (line == null)
                    break;
                System.out.println(line);
            }
        }
        int exitCode = p.waitFor();
        if (exitCode != 0)
            throw new IOException("Error running script " + script + ": " + exitCode);
    }
}
