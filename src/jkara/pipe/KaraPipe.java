package jkara.pipe;

import jkara.ass.AssSync;
import jkara.scroll.AssJoiner;
import jkara.sync.FastText;
import jkara.sync.SyncException;
import jkara.sync.TextSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                String output = Stream.of(files).map(f -> f.input().getFileName().toString()).collect(Collectors.joining(", "));
                log(String.format("%s (building '%s' because %s)...", log, output, cause));
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
            "--write-info-json", "-k",
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
     * 1. audio.mp3 -> demucs -> vocals.wav + no_vocals.wav
     * 2. vocals.wav -> fast_whisper -> fast.json (semi-accurate transcription)
     * 3. text.txt + fast.json -> TextSync -> text.json (real lyrics with timestamps from prev step)
     * 4. text.json -> whisperx -> aligned.json (character-level timestamps for real lyrics)
     * 5. aligned.json + text.txt -> AssSync -> subs.ass (line-by-line subtitles suitable for manual editing)
     * 6. subs.ass + info.json -> AssJoiner -> karaoke.ass (ready for karaoke subtitles)
     * 7. no_vocals.wav + karaoke.ass -> ffmpeg -> karaoke.mp4
     */
    public void makeKaraoke(Path audio, String maybeShifts, String maybeLanguage, Path userText, Path workDir) throws IOException, InterruptedException, SyncException {
        long t0 = System.currentTimeMillis();
        Files.createDirectories(workDir);
        Stages stages = new Stages(workDir);

        String baseName = nameWithoutExtension(audio);
        String demucsDir = "htdemucs/" + baseName;
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
            text = new StageFile(userText, false);
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
        if (isStage("Performing alignment", alignedJson)) {
            runner.runPython(
                "scripts/align.py",
                vocals.input().toString(), textJson.input().toString(), alignedJson.output().toString()
            );
        }

        StageFile ass = stages.file("subs.ass", alignedJson, text);
        if (isStage("Creating subtitles", ass)) {
            AssSync.sync(
                text.input(), textJson.input(), alignedJson.input(),
                () -> Files.newBufferedWriter(ass.output())
            );
        }

        StageFile infoJson = new StageFile(audio.resolveSibling(audio.getFileName() + ".info.json"), true);
        StageFile scroll = stages.file("karaoke.ass", ass, infoJson);
        if (isStage("Creating karaoke subtitles", scroll)) {
            AssJoiner.join(ass.input(), infoJson.input(), scroll.output());
        }
        StageFile karaoke = stages.file("karaoke.mp4", noVocals, scroll);
        if (isStage("Building karaoke video", karaoke)) {
            List<String> videoInput;
            Path video = audio.resolveSibling(baseName + ".webm");
            if (Files.exists(video)) {
                videoInput = List.of("-i", video.toString());
            } else {
                // Используем виртуальное видео длиной 1 час, оно обрезается с помощью опции -shortest до длины аудио:
                videoInput = List.of(
                    "-f", "lavfi", "-i", "color=size=1280x720:duration=3600:rate=24:color=black"
                );
            }
            List<String> audioInput = List.of(
                "-i", noVocals.input().toString()
            );

            List<String> ffmpeg = new ArrayList<>();
            ffmpeg.addAll(videoInput); // input 0
            ffmpeg.addAll(audioInput); // input 1
            ffmpeg.addAll(List.of(
                "-y", "-stats",
                "-v", "quiet",
                "-vf", "ass=" + escapeFilter(scroll.input().toString()),
                "-map", "0:v:0", // video from input 0
                "-map", "1:a:0", // audio from input 1
                "-shortest",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-b:a", "192k",
                karaoke.output().toString()
            ));
            runner.runFFMPEG(ffmpeg);
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
