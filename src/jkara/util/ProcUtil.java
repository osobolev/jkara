package jkara.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

public final class ProcUtil {

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void log(String format, Object... args) {
        Object[] normalized;
        if (args.length > 0) {
            normalized = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof Path path) {
                    normalized[i] = path.normalize();
                } else {
                    normalized[i] = arg;
                }
            }
        } else {
            normalized = args;
        }
        String message = format.formatted(normalized);
        System.out.printf(">>>>> [%s] %s%n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), message);
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
    public static void runCommand(String what, Path exe, List<String> args, List<Path> pathDirs,
                                  Consumer<Process> out, IntPredicate exitOk) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(exe.toString());
        command.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(command);
        if (!pathDirs.isEmpty()) {
            String pathEnv = "PATH";
            Set<String> pathLike = new HashSet<>();
            for (String name : pb.environment().keySet()) {
                if (pathEnv.equalsIgnoreCase(name)) {
                    pathLike.add(name);
                }
            }
            if (pathLike.isEmpty()) {
                pathLike.add(pathEnv);
            }
            String addPath = pathDirs.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
            for (String name : pathLike) {
                String pathValue = pb.environment().get(name);
                pb.environment().put(name, pathValue == null ? addPath : addPath + File.pathSeparator + pathValue);
            }
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
}
