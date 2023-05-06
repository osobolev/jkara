package jkara.pipe;

import jkara.ass.AssSync;
import jkara.sync.TextSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class KaraPipe {

    private static String escapeFilter(String path) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '\\') {
                buf.append('/');
            } else if (ch == ':') {
                buf.append("\\\\:");
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    /**
     * Pipeline:
     * vocals.wav -> fast_whisper -> fast.json (semi-accurate transcription)
     * text.txt + fast.json -> TextSync -> text.json (real lyrics with timestamps from prev step)
     * text.json -> whisperx -> aligned.json (character-level timestamps for real lyrics)
     * aligned.json + text.txt -> AssSync -> ass file
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        Path audio = Path.of("C:\\home\\projects\\my\\kara\\work\\PjdEDOIai8Y.mp3");
        Path text = Path.of("C:\\home\\projects\\my\\kara\\work\\text.txt");

        Path ffmpeg = Path.of("ffmpeg").toAbsolutePath(); // todo
        ProcRunner runner = new ProcRunner(ffmpeg);

        Path dir = Path.of("work"); // todo
        Files.createDirectories(dir);

        // todo: run youtube-download
        // todo: run demucs

        Path vocals = Path.of("C:\\home\\projects\\my\\kara\\work\\separated\\htdemucs\\PjdEDOIai8Y\\vocals.wav");
        Path noVocals = Path.of("C:\\home\\projects\\my\\kara\\work\\separated\\htdemucs\\PjdEDOIai8Y\\no_vocals.wav");
        Path text = Path.of("C:\\home\\projects\\my\\kara\\work\\text.txt");

        Path fastJson = dir.resolve("fast.json");
        if (!Files.exists(fastJson)) {
            runner.runPython("scripts/transcribe.py", vocals.toString(), fastJson.toString());
        }
        Path textJson = dir.resolve("text.json");
        if (!Files.exists(textJson)) {
            TextSync.sync(text, fastJson, () -> Files.newBufferedWriter(textJson));
        }
        Path alignedJson = dir.resolve("aligned.json");
        if (!Files.exists(alignedJson)) {
            runner.runPython("scripts/align.py", vocals.toString(), textJson.toString(), alignedJson.toString());
        }
        Path ass = dir.resolve("subs.ass");
        if (!Files.exists(ass)) {
            AssSync.sync(text, alignedJson, () -> Files.newBufferedWriter(ass));
        }
        Path karaoke = dir.resolve("karaoke.mp4");
        if (!Files.exists(karaoke)) {
            Path tmpDuration = Files.createTempFile("duration", ".txt");
            String duration;
            try {
                runner.runPython("scripts/duration.py", noVocals.toString(), tmpDuration.toString());
                duration = Files.readString(tmpDuration);
            } finally {
                Files.deleteIfExists(tmpDuration);
            }

            runner.runFFMPEG(
                "-y",
                "-f", "lavfi",
                "-i", String.format("color=size=1280x720:duration=%s:rate=24:color=black", duration),
                "-i", noVocals.toString(),
                "-vf", "ass=" + escapeFilter(ass.toString()),
                "-shortest",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-b:a", "192k",
                karaoke.toString()
            );
        }
    }
}
