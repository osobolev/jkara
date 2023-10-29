package jkara;

import jkara.opts.OptFile;
import jkara.pipe.KaraPipe;
import jkara.setup.Tools;
import jkara.sync.SyncException;
import jkara.util.ProcUtil;

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

        ProcUtil.registerShutdown();

        try {
            Tools tools = Tools.setup(cmd.rootDir);
            KaraPipe pipe = new KaraPipe(cmd.rootDir, tools);
            if (cmd.args instanceof CmdArgs.Options) {
                pipe.copyOptions(cmd.dir);
                return;
            }
            Path inputFile = cmd.dir.resolve("input.properties");
            OInput prevInput = OptFile.read(OInput.class, inputFile).value;
            boolean newProject = prevInput.audio() == null;
            Path audio;
            String language;
            Path text;
            if (cmd.args instanceof CmdArgs.Empty) {
                if (newProject) {
                    CmdArgs.help(null);
                    System.exit(1);
                    return;
                } else {
                    audio = cmd.dir.resolve(prevInput.audio());
                    language = prevInput.language();
                    text = cmd.dir.resolve(prevInput.text());
                }
            } else if (cmd.args instanceof CmdArgs.Input inp) {
                OInput newInput = OInput.create(cmd.dir, inp.url(), inp.audio(), inp.language(), inp.text());
                if (newProject) {
                    OptFile.write(inputFile, newInput);
                } else {
                    List<String> diff = OInput.diff(prevInput, newInput);
                    if (!diff.isEmpty()) {
                        throw new InitException(String.format("Input properties %s changed relative to saved in '%s'", diff, inputFile));
                    }
                }
                String url = inp.url();
                audio = inp.audio();
                language = inp.language();
                text = inp.text();

                if (url != null) {
                    pipe.downloadYoutube(url, audio, newProject);
                }
            } else {
                return;
            }
            pipe.makeKaraoke(audio, language, text, cmd.dir);
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
