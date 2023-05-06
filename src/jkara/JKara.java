package jkara;

import jkara.pipe.KaraPipe;
import jkara.sync.SyncException;

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
            KaraPipe pipe = new KaraPipe(null, cmd.rootDir);
            if (cmd.url != null) {
                pipe.downloadYoutube(cmd.url, cmd.audio);
            }
            pipe.makeKaraoke(cmd.audio, cmd.shifts, cmd.language, cmd.text, cmd.dir);
        } catch (SyncException ex) {
            System.err.println(ex.getMessage());
            System.err.printf("Please fix %s manually%n", ex.fastJson);
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
