package jkara.pipe;

import jkara.ass.AssSync;
import jkara.sync.TextSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KaraPipe {

    private final ProcRunner runner;

    public KaraPipe(Path ffmpeg) {
        this.runner = new ProcRunner(ffmpeg);
    }

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
     * audio.mp3 -> demucs -> vocals.wav + no_vocals.wav
     * vocals.wav -> fast_whisper -> fast.json (semi-accurate transcription)
     * text.txt + fast.json -> TextSync -> text.json (real lyrics with timestamps from prev step)
     * text.json -> whisperx -> aligned.json (character-level timestamps for real lyrics)
     * aligned.json + text.txt -> AssSync -> ass file
     * no_vocals.wav + ass file -> ffmpeg -> karaoke.mp4
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void makeKaraoke(Path audio, String maybeLanguage, Path text, Path workDir) throws IOException, InterruptedException {
        Files.createDirectories(workDir);

        String audioName = audio.getFileName().toString();
        int dot = audioName.lastIndexOf('.');
        String id = dot < 0 ? audioName : audioName.substring(0, dot);
        Path demucsDir = workDir.resolve("htdemucs/" + id);
        Path vocals = demucsDir.resolve("vocals.wav");
        Path noVocals = demucsDir.resolve("no_vocals.wav");
        if (!Files.exists(vocals) || !Files.exists(noVocals)) {
            System.out.println("Separating vocals and instrumental...");
            String shifts = "0"; // todo
            runner.runExe(
                "demucs",
                "--two-stems=vocals",
                "--shifts=" + shifts,
                "--out=" + workDir,
                audio.toString()
            );
        }

        Path fastJson = workDir.resolve("fast.json");
        if (!Files.exists(fastJson)) {
            System.out.println("Transcribing vocals...");
            String language = maybeLanguage == null ? "-" : maybeLanguage;
            runner.runPython("scripts/transcribe.py", vocals.toString(), fastJson.toString(), language);
        }
        Path textJson = workDir.resolve("text.json");
        if (!Files.exists(textJson)) {
            System.out.println("Synchronizing transcription with text...");
            TextSync.sync(text, fastJson, () -> Files.newBufferedWriter(textJson));
        }
        Path alignedJson = workDir.resolve("aligned.json");
        if (!Files.exists(alignedJson)) {
            System.out.println("Performing alignment...");
            runner.runPython("scripts/align.py", vocals.toString(), textJson.toString(), alignedJson.toString());
        }
        Path ass = workDir.resolve("subs.ass");
        if (!Files.exists(ass)) {
            System.out.println("Creating subtitles...");
            AssSync.sync(text, alignedJson, () -> Files.newBufferedWriter(ass));
        }
        Path karaoke = workDir.resolve("karaoke.mp4");
        if (!Files.exists(karaoke)) {
            System.out.println("Building karaoke video...");
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
        System.out.println("Done: " + karaoke);
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
        Path workDir = Path.of("work"); // todo

        Path ffmpeg = Path.of("ffmpeg").toAbsolutePath(); // todo
        KaraPipe pipe = new KaraPipe(ffmpeg);
        pipe.makeKaraoke(audio, "en", text, workDir);
    }
}
