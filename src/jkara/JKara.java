package jkara;

import jkara.pipe.KaraPipe;

import java.io.IOException;
import java.nio.file.Path;

public final class JKara {

    public static void main(String[] args) throws IOException, InterruptedException {
        Path audio = Path.of("C:\\home\\projects\\my\\kara\\work\\PjdEDOIai8Y.mp3");
        Path text = Path.of("C:\\home\\projects\\my\\kara\\work\\text.txt");
        Path workDir = Path.of("work"); // todo

        Path ffmpeg = Path.of("ffmpeg").toAbsolutePath(); // todo
        KaraPipe pipe = new KaraPipe(ffmpeg);
        pipe.downloadYoutube("https://www.youtube.com/watch?v=PjdEDOIai8Y", workDir.resolve("selfmachine.mp3"));
        pipe.makeKaraoke(audio, "en", text, workDir);
    }
}
