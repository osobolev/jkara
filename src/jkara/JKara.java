package jkara;

import jkara.pipe.KaraPipe;

import java.nio.file.Path;

public final class JKara {

    /**
     * [-l language] [-d dir] [-f file] URL text
     * [-l language] [-d dir] audio text
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        CmdArgs cmd = CmdArgs.parse(args);
        if (cmd == null) {
            System.exit(1);
            return;
        }

        try {
            KaraPipe pipe = new KaraPipe(null);
            if (cmd.url != null) {
                pipe.downloadYoutube(cmd.url, cmd.audio);
            }
            pipe.makeKaraoke(cmd.audio, cmd.language, cmd.text, cmd.dir);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
