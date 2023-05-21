package jkara;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

final class CmdArgs {

    private static final String DEFAULT_AUDIO = "audio.mp3";

    final Path rootDir;
    final Path dir;
    final String url;
    final Path audio;
    final String language;
    final Path text;

    private CmdArgs(Path rootDir, Path dir, String url, Path audio, String language, Path text) {
        this.rootDir = rootDir;
        this.dir = dir;
        this.url = url;
        this.audio = audio;
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
        System.out.println("    jkara [-l <language>] [-d <dir>] [-f <file>] <URL> [<text>]");
        System.out.println("    jkara [-l <language>] [-d <dir>] [<audio>|<audio> <text>]");
        System.out.println("    jkara [-d <dir>] options");
        System.out.println("where");
        System.out.println("    language: en/de/fr/...");
        if (error != null) {
            System.out.println();
            System.out.println(error);
        }
    }

    private static CmdArgs error(String message) throws ArgException {
        throw new ArgException(message);
    }

    private static Path dir(String dirStr) {
        return dirStr == null ? Path.of(".") : Path.of(dirStr);
    }

    private static void checkMP3(Path audio) throws ArgException {
        byte[][] magics = {
            {0x49, 0x44, 0x33}
        };
        int maxMagic = 0;
        for (byte[] magic : magics) {
            maxMagic = Math.max(maxMagic, magic.length);
        }
        try (InputStream is = Files.newInputStream(audio)) {
            byte[] bytes = is.readNBytes(maxMagic);
            boolean anyMagic = Stream.of(magics).anyMatch(
                magic -> Arrays.equals(bytes, 0, magic.length, magic, 0, magic.length)
            );
            if (!anyMagic) {
                error(String.format("File '%s' is not an MP3 file", audio));
            }
        } catch (IOException ex) {
            error(String.format("Cannot open file '%s'", audio));
        }
    }

    private static CmdArgs doParse(String[] args) throws ArgException {
        List<String> positionalArgs = new ArrayList<>();
        String rootDirArg = null;
        String dirArg = null;
        String fileArg = null;
        String language = null;
        for (int i = 0; i < args.length; ) {
            String arg = args[i];
            if (arg.startsWith("-") && i + 1 < args.length) {
                String value = args[i + 1];
                switch (arg) {
                case "-r" -> rootDirArg = value;
                case "-d" -> dirArg = value;
                case "-f" -> fileArg = value;
                case "-l" -> language = value;
                default -> error("Unexpected option " + arg);
                }
                i += 2;
            } else {
                positionalArgs.add(arg);
                i++;
            }
        }
        if (positionalArgs.size() > 2) {
            return error("Must have one (audio) or two (audio+text) positional arguments");
        }

        Path rootDir = dir(rootDirArg);
        Path dir = dir(dirArg);

        if (positionalArgs.size() == 1 && "options".equalsIgnoreCase(positionalArgs.get(0))) {
            return new CmdArgs(rootDir, dir, null, null, null, null);
        }

        String urlOrFile;
        if (positionalArgs.size() > 0) {
            urlOrFile = positionalArgs.get(0);
        } else {
            urlOrFile = null;
        }
        Path audio;
        String url;
        if (urlOrFile != null) {
            boolean isURL;
            try {
                new URL(urlOrFile);
                isURL = true;
            } catch (MalformedURLException ex) {
                isURL = false;
            }
            if (isURL) {
                url = urlOrFile;
                if (fileArg == null) {
                    audio = dir.resolve(DEFAULT_AUDIO);
                } else {
                    audio = Path.of(fileArg);
                }
            } else {
                url = null;
                audio = Path.of(urlOrFile);
                if (!Files.exists(audio)) {
                    return error(String.format("File '%s' does not exist", audio));
                } else {
                    checkMP3(audio);
                }
            }
        } else {
            url = null;
            audio = dir.resolve(DEFAULT_AUDIO);
            if (!Files.exists(audio)) {
                return error(String.format("Default file '%s' does not exist, copy it or download from Youtube", audio));
            } else {
                checkMP3(audio);
            }
        }

        Path text;
        if (positionalArgs.size() > 1) {
            text = Path.of(positionalArgs.get(1));
            if (!Files.exists(text)) {
                return error(String.format("File '%s' does not exist", text));
            }
        } else {
            text = dir.resolve("text.txt");
        }

        return new CmdArgs(rootDir, dir, url, audio, language, text);
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
