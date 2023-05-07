package jkara.pipe;

import jkara.ass.AssSync;
import jkara.sync.FastText;
import jkara.sync.SyncException;
import jkara.sync.TextSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class KaraPipe {

    private final ProcRunner runner;

    public KaraPipe(Path ffmpeg, Path scriptDir) {
        this.runner = new ProcRunner(ffmpeg, scriptDir);
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

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void log(String message) {
        System.out.printf(">>>>> [%s] %s%n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), message);
    }

    private static boolean isStage(String log, StageFile... files) {
        for (StageFile file : files) {
            String cause = file.isModified();
            if (cause != null) {
                log(log + " (cause: " + cause + ")...");
                return true;
            }
        }
        return false;
    }

    public void downloadYoutube(String url, Path audio) throws IOException, InterruptedException {
        log("Downloading from Youtube...");
        Files.createDirectories(audio.getParent());
        runner.runExe(
            "yt-dlp",
            "--extract-audio",
            "--audio-format", "mp3",
            "--output", audio.toString(),
            url
        );
        log(String.format("Audio downloaded to %s", audio));
    }

    private static String nameWithoutExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
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
    public void makeKaraoke(Path audio, String maybeShifts, String maybeLanguage, Path userText, Path workDir) throws IOException, InterruptedException, SyncException {
        long t0 = System.currentTimeMillis();
        Files.createDirectories(workDir);
        Stages stages = new Stages(workDir);

        String id = nameWithoutExtension(audio);
        String demucsDir = "htdemucs/" + id;
        StageFile vocals = stages.file(demucsDir + "/vocals.wav");
        StageFile noVocals = stages.file(demucsDir + "/no_vocals.wav");
        if (isStage("Separating vocals and instrumental", vocals, noVocals)) {
            String shifts = maybeShifts == null ? "1" : maybeShifts;
            runner.runExe(
                "demucs",
                "--two-stems=vocals",
                "--shifts=" + shifts,
                "--out=" + workDir,
                audio.toString()
            );
            vocals.output();
            noVocals.output();
        }

        StageFile fastJson = stages.file("fast.json", vocals);
        if (isStage("Transcribing vocals", fastJson)) {
            String language = maybeLanguage == null ? "-" : maybeLanguage;
            runner.runPython(
                "scripts/transcribe.py",
                vocals.input().toString(), fastJson.output().toString(), language
            );
        }
        StageFile text;
        if (userText != null) {
            text = new StageFile(userText);
        } else {
            text = stages.file("text.txt");
            if (isStage("Saving transcribed text (you can edit it)", text)) {
                FastText.textFromFast(fastJson.input(), text.output());
            }
        }
        StageFile textJson = stages.file("text.json", text, fastJson);
        if (isStage("Synchronizing transcription with text", textJson)) {
            TextSync.sync(text.input(), fastJson.input(), () -> Files.newBufferedWriter(textJson.output()));
        }
        StageFile alignedJson = stages.file("aligned.json", vocals, textJson);
        if (isStage("Performing alignment...", alignedJson)) {
            runner.runPython(
                "scripts/align.py",
                vocals.input().toString(), textJson.input().toString(), alignedJson.output().toString()
            );
        }
        StageFile ass = stages.file("subs.ass", text, alignedJson);
        if (isStage("Creating subtitles", ass)) {
            AssSync.sync(text.input(), alignedJson.input(), () -> Files.newBufferedWriter(ass.output()));
        }
        StageFile scroll = stages.file("karaoke.ass", ass);
        if (isStage("Creating karaoke subtitles", scroll)) {
            // todo: modify subs for scrolling:
            Files.copy(ass.input(), scroll.output());
        }
        StageFile karaoke = stages.file("karaoke.mp4", noVocals, scroll);
        if (isStage("Building karaoke video", karaoke)) {
            Path tmpDuration = Files.createTempFile("duration", ".txt");
            String duration;
            try {
                runner.runPython(
                    "scripts/duration.py",
                    noVocals.input().toString(), tmpDuration.toString()
                );
                duration = Files.readString(tmpDuration);
            } finally {
                Files.deleteIfExists(tmpDuration);
            }

            runner.runFFMPEG(
                "-y", "-stats",
                "-v", "quiet",
                "-f", "lavfi",
                "-i", String.format("color=size=1280x720:duration=%s:rate=24:color=black", duration),
                "-i", noVocals.input().toString(),
                "-vf", "ass=" + escapeFilter(scroll.input().toString()),
                "-shortest",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-b:a", "192k",
                karaoke.output().toString()
            );
        }
        long t1 = System.currentTimeMillis();
        log(String.format("Done in %s, result written to %s", duration(t1 - t0), karaoke.input()));
    }

    private static String duration(long millis) {
        long totalSeconds = millis / 1000;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long hours = totalMinutes / 60;
        if (hours > 0) {
            return String.format("%s:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%s:%02d", minutes, seconds);
        }
    }
}
