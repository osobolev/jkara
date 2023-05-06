package jkara;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CmdArgs {

    final Path rootDir;
    final Path dir;
    final String url;
    final Path audio;
    final String shifts;
    final String language;
    final Path text;

    private CmdArgs(Path rootDir, Path dir, String url, Path audio, String shifts, String language, Path text) {
        this.rootDir = rootDir;
        this.dir = dir;
        this.url = url;
        this.audio = audio;
        this.shifts = shifts;
        this.language = language;
        this.text = text;
    }

    private static final class ArgException extends Exception {

        ArgException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void help(String error) {
        System.out.println("Usage:");
        System.out.println("    jkara [-l language] [-s shifts] [-d dir] [-f file] URL [text]");
        System.out.println("    jkara [-l language] [-s shifts] [-d dir] audio [text]");
        System.out.println("where");
        System.out.println("    language: en/de/fr/...");
        System.out.println("    shifts: used in vocals separation (more -> better quality/slower)");
        if (error != null) {
            System.out.println();
            System.out.println(error);
        }
    }

    private static CmdArgs error(String message) throws ArgException {
        throw new ArgException(message);
    }

    private static CmdArgs doParse(String[] args) throws ArgException {
        List<String> positionalArgs = new ArrayList<>();
        String rootDirArg = null;
        String dirArg = null;
        String fileArg = null;
        String langArg = null;
        String shiftsArg = null;
        for (int i = 0; i < args.length; ) {
            String arg = args[i];
            if (arg.startsWith("-") && i + 1 < args.length) {
                String value = args[i + 1];
                switch (arg) {
                case "-r" -> rootDirArg = value;
                case "-d" -> dirArg = value;
                case "-f" -> fileArg = value;
                case "-l" -> langArg = value;
                case "-s" -> shiftsArg = value;
                default -> error("Unexpected option " + arg);
                }
                i += 2;
            } else {
                positionalArgs.add(arg);
                i++;
            }
        }
        if (positionalArgs.size() != 1 && positionalArgs.size() != 2) {
            return error("Must have one (audio) or two (audio+text) positional arguments");
        }
        String urlOrFile = positionalArgs.get(0);
        Path text;
        if (positionalArgs.size() > 1) {
            text = Path.of(positionalArgs.get(1));
            if (!Files.exists(text)) {
                return error(String.format("File '%s' does not exist", text));
            }
        } else {
            text = null;
        }
        Path dir = dirArg == null ? Path.of(".") : Path.of(dirArg);
        boolean isURL;
        try {
            new URL(urlOrFile);
            isURL = true;
        } catch (MalformedURLException ex) {
            isURL = false;
        }
        Path audio;
        String url;
        if (isURL) {
            url = urlOrFile;
            if (fileArg == null) {
                audio = dir.resolve("audio.mp3");
            } else {
                audio = Path.of(fileArg);
            }
        } else {
            url = null;
            audio = Path.of(urlOrFile);
            if (!Files.exists(audio)) {
                return error(String.format("File '%s' does not exist", audio));
            }
        }
        Path rootDir = rootDirArg == null ? Path.of(".") : Path.of(rootDirArg);
        return new CmdArgs(rootDir, dir, url, audio, shiftsArg, langArg, text);
    }

    static CmdArgs parse(String[] args) {
        try {
            return doParse(args);
        } catch (ArgException ex) {
            help(ex.getMessage());
            return null;
        }
    }
}
