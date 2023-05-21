package jkara;

import jkara.opts.OptFile;
import jkara.pipe.KaraPipe;
import jkara.sync.SyncException;

import java.nio.file.Path;
import java.util.List;

public final class JKara {

    private static final class InitException extends Exception {

        InitException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        CmdArgs cmd = CmdArgs.parse(args);
        if (cmd == null) {
            System.exit(1);
            return;
        }

        try {
            Path inputFile = cmd.dir.resolve("input.properties");
            OInput newInput = OInput.create(cmd.dir, cmd.url, cmd.audio, cmd.language, cmd.text);
            OInput prevInput = OptFile.read(OInput.class, inputFile).value;
            boolean newProject = prevInput.audio() == null;
            if (newProject) {
                OptFile.write(inputFile, newInput);
            } else {
                List<String> diff = OInput.diff(prevInput, newInput);
                if (!diff.isEmpty()) {
                    throw new InitException(String.format("Input properties %s changed relative to saved in '%s'", diff, inputFile));
                }
            }

            KaraPipe pipe = new KaraPipe(null, cmd.rootDir);
            if (cmd.url != null) {
                pipe.downloadYoutube(cmd.url, cmd.audio);
            }
            pipe.makeKaraoke(cmd.audio, cmd.language, cmd.text, cmd.dir);
        } catch (SyncException ex) {
            System.err.println(ex.getMessage());
            System.err.printf("Please fix %s manually%n", ex.fastJson);
            System.exit(1);
        } catch (InitException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
