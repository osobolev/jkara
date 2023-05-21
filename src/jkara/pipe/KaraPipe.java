package jkara.pipe;

import jkara.ass.AssSync;
import jkara.opts.ODemucs;
import jkara.opts.OptFile;
import jkara.scroll.AssJoiner;
import jkara.sync.FastText;
import jkara.sync.SyncException;
import jkara.sync.TextSync;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class KaraPipe {

    private static final String DEMUCS_OPTS = "demucs";
    private static final String[] ALL_OPTS = {DEMUCS_OPTS};

    private final Path rootDir;
    private final ProcRunner runner;

    public KaraPipe(Path ffmpeg, Path rootDir) {
        this.rootDir = rootDir;
        this.runner = new ProcRunner(ffmpeg, rootDir);
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

    private static Map<String, String> queryParams(String url) {
        Map<String, String> params = new HashMap<>();
        int q = url.indexOf('?');
        if (q < 0)
            return params;
        String[] pairs = url.substring(q + 1).split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0)
                continue;
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private static int parse(StringBuilder buf, String unit) {
        int pos = buf.indexOf(unit);
        if (pos > 0) {
            int value = Integer.parseInt(buf.substring(0, pos));
            buf.delete(0, pos + 1);
            return value;
        } else {
            return 0;
        }
    }

    private static long parseTime(String t) {
        StringBuilder buf = new StringBuilder(t.trim());
        long hours = parse(buf, "h");
        long minutes = parse(buf, "m");
        long seconds = parse(buf, "s");
        if (!buf.isEmpty() && hours == 0 && minutes == 0 && seconds == 0) {
            return Integer.parseInt(buf.toString());
        }
        return hours * 60 * 60 + minutes * 60 + seconds;
    }

    private static String range(String url) {
        Map<String, String> params = queryParams(url);
        String t = params.get("t");
        String start = params.get("start");
        String end = params.get("end");
        Long secStart = null;
        if (t != null) {
            secStart = parseTime(t);
        } else if (start != null) {
            secStart = parseTime(start);
        }
        Long secEnd = null;
        if (end != null) {
            secEnd = parseTime(end);
        }
        if (secStart == null && secEnd == null)
            return null;
        StringBuilder buf = new StringBuilder();
        if (secStart != null) {
            buf.append(duration(secStart.longValue() * 1000L));
        } else {
            buf.append("0:00");
        }
        buf.append('-');
        if (secEnd != null) {
            buf.append(duration(secEnd.longValue() * 1000L));
        } else {
            buf.append("inf");
        }
        return buf.toString();
    }

    public void downloadYoutube(String url, Path audio, boolean newProject) throws IOException, InterruptedException {
        if (!newProject && Files.exists(audio)) {
            log("Skipping downloading from Youtube as it is a repeated run");
            return;
        }
        String range = range(url);
        log(String.format("Downloading from Youtube%s...", range == null ? "" : " (range " + range + ")"));
        Files.createDirectories(audio.getParent());
        List<String> args = new ArrayList<>(List.of(
            "--write-info-json", "-k",
            "--extract-audio",
            "--audio-format", "mp3",
            "--output", audio.toString()
        ));
        if (range != null) {
            args.addAll(List.of("--download-sections", "*" + range));
        }
        args.add(url);
        runner.runExe("yt-dlp", args);
        log(String.format("Audio downloaded to %s", audio));
    }

    private static String nameWithoutExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    /**
     * Pipeline:
     * 1. audio.mp3 -> demucs -> vocals.wav + no_vocals.wav
     * 2. vocals.wav -> fast_whisper -> fast.json (semi-accurate transcription)
     * 3. text.txt + fast.json -> TextSync -> text.json (real lyrics with timestamps from prev step)
     * 4. text.json + vocals.wav -> whisperx -> aligned.json (character-level timestamps for real lyrics)
     * 5. aligned.json + text.txt -> AssSync -> subs.ass (line-by-line subtitles suitable for manual editing)
     * 6. subs.ass + [info.json] -> AssJoiner -> karaoke.ass (ready for karaoke subtitles)
     * 7. no_vocals.wav + karaoke.ass + [audio.webm] -> ffmpeg -> karaoke.mp4
     */
    public void makeKaraoke(Path audioInput, String maybeLanguage, Path textInput, Path workDir) throws IOException, InterruptedException, SyncException {
        long t0 = System.currentTimeMillis();
        Files.createDirectories(workDir);
        Stages stages = new Stages(rootDir, workDir);

        StageFile audio = new StageFile(audioInput);

        String audioName = audio.input().getFileName().toString();
        String baseName = nameWithoutExtension(audioName);
        String demucsDir = "htdemucs/" + baseName;
        StageValue<ODemucs> demucsOpts = stages.options(DEMUCS_OPTS, ODemucs.class);
        StageFile vocals = stages.file(demucsDir + "/vocals.wav", audio, demucsOpts);
        StageFile noVocals = stages.file(demucsDir + "/no_vocals.wav", audio, demucsOpts);
        if (isStage("Separating vocals and instrumental", vocals, noVocals)) {
            runner.runExe(
                "demucs",
                "--two-stems=vocals",
                "--shifts=" + demucsOpts.value().shifts(),
                "--out=" + workDir,
                audio.input().toString()
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

        StageFile text = new StageFile(textInput);
        if (isStage("Saving transcribed text (you can edit it)", text)) {
            FastText.textFromFast(fastJson.input(), text.output());
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

        StageFile infoJson = new StageFile(audio.input().resolveSibling(audioName + ".info.json"), true);
        StageFile scroll = stages.file("karaoke.ass", ass, infoJson);
        if (isStage("Creating karaoke subtitles", scroll)) {
            AssJoiner.join(ass.input(), infoJson.input(), scroll.output());
        }

        StageFile video = new StageFile(audio.input().resolveSibling(baseName + ".webm"), true);
        StageFile karaoke = stages.file("karaoke.mp4", noVocals, scroll, video);
        if (isStage("Building karaoke video", karaoke)) {
            makeVideo(video.input(), noVocals.input(), scroll.input(), karaoke.output());
        }

        long t1 = System.currentTimeMillis();
        log(String.format("Done in %s, result written to %s", duration(t1 - t0), karaoke.input()));
    }

    private void makeVideo(Path video, Path noVocals, Path scroll, Path karaoke) throws IOException, InterruptedException {
        List<String> videoInput;
        if (Files.exists(video)) {
            videoInput = List.of("-i", video.toString());
        } else {
            // Используем виртуальное видео длиной 1 час, оно обрезается с помощью опции -shortest до длины аудио:
            videoInput = List.of(
                "-f", "lavfi", "-i", "color=size=1280x720:duration=3600:rate=24:color=black"
            );
        }
        List<String> audioInput = List.of(
            "-i", noVocals.toString()
        );

        List<String> ffmpeg = new ArrayList<>();
        ffmpeg.addAll(videoInput); // input 0
        ffmpeg.addAll(audioInput); // input 1
        ffmpeg.addAll(List.of(
            "-y", "-stats",
            "-v", "quiet",
            "-vf", "ass=" + escapeFilter(scroll.toString()),
            "-map", "0:v:0", // video from input 0
            "-map", "1:a:0", // audio from input 1
            "-shortest",
            "-c:v", "libx264", // todo: use copy for real video???
            "-c:a", "aac", // todo: use same codec as source???
            "-b:a", "192k", // todo: use same bitrate as source???
            karaoke.toString()
        ));
        runner.runFFMPEG(ffmpeg);
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

    public void copyOptions(Path workDir) throws IOException {
        for (String opt : ALL_OPTS) {
            Path from = OptFile.path(rootDir, opt);
            if (!Files.exists(from))
                continue;
            Path to = OptFile.path(workDir, opt);
            Files.copy(from, to);
        }
    }
}
