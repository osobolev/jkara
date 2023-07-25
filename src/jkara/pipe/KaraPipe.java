package jkara.pipe;

import jkara.ass.AssSync;
import jkara.opts.ODemucs;
import jkara.opts.OKaraoke;
import jkara.opts.OptFile;
import jkara.scroll.AssJoiner;
import jkara.setup.Tools;
import jkara.sync.FastText;
import jkara.sync.SyncException;
import jkara.sync.TextSync;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jkara.util.ProcUtil.log;

public final class KaraPipe {

    private static final String DEMUCS_OPTS = "demucs";
    private static final String KARAOKE_OPTS = "karaoke";
    private static final String[] ALL_OPTS = {DEMUCS_OPTS, KARAOKE_OPTS};

    private final Path rootDir;
    private final ProcRunner runner;

    public KaraPipe(Path rootDir, Tools tools) {
        this.rootDir = rootDir;
        this.runner = new ProcRunner(tools, rootDir);
    }

    private interface NameParts {

        String process(String baseName, String ext);
    }

    private static String splitName(Path file, NameParts parts) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return parts.process(name, "");
        } else {
            return parts.process(name.substring(0, dot), name.substring(dot));
        }
    }

    private static String nameWithoutExtension(Path file) {
        return splitName(file, (baseName, ext) -> baseName);
    }

    private interface NameCandidates {

        Stream<String> getNames(String audioName, String baseName);
    }

    private static Path oneOfSiblings(Path audio, NameCandidates getNames) {
        String audioName = audio.getFileName().toString();
        String baseName = nameWithoutExtension(audio);
        Iterator<String> it = getNames.getNames(audioName, baseName).iterator();
        Path first = null;
        while (it.hasNext()) {
            String name = it.next();
            Path path = audio.resolveSibling(name);
            if (Files.exists(path))
                return path;
            if (first == null) {
                first = path;
            }
        }
        return first;
    }

    private static Path video(Path audio) {
        return oneOfSiblings(audio, (audioName, baseName) -> Stream.of(".webm", ".mp4").map(ext -> baseName + ext));
    }

    private static Path info(Path audio) {
        return oneOfSiblings(audio, (audioName, baseName) -> Stream.of(audioName, baseName).map(name -> name + ".info.json"));
    }

    public void downloadYoutube(String url, Path audio, boolean newProject) throws IOException, InterruptedException {
        if (!newProject && Files.exists(audio)) {
            log("Skipping downloading from Youtube as it is a repeated run");
            return;
        }
        CutRange range = CutRange.create(url);
        log(String.format("Downloading from Youtube%s...", range == null ? "" : " (range " + range + ")"));
        Files.createDirectories(audio.getParent());
        if (range == null) {
            runner.runPythonExe(
                "yt-dlp",
                "--write-info-json", "-k",
                "--extract-audio",
                "--audio-format", "mp3",
                "--output", audio.toString(),
                url
            );
        } else {
            Path video = video(audio);
            runner.runPythonExe(
                "yt-dlp",
                "--write-info-json",
                "--output", video.toString(),
                url
            );
            log("Cutting downloaded video...");
            CutRange realCut = range.getRealCut(runner, video);
            String tmpName = splitName(video, (baseName, ext) -> baseName + ".tmp" + ext);
            Path cutVideo = video.resolveSibling(tmpName);
            realCut.doCut(runner, video, cutVideo);
            Files.delete(video);
            Files.move(cutVideo, video);
            runner.runFFMPEG(List.of(
                "-y", "-stats",
                "-i", video.toString(),
                "-vn",
                "-b:a", "192k",
                "-f", "mp3",
                audio.toString()
            ));
        }
        log(String.format("Audio downloaded to %s", audio));
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

        String demucsDir = "htdemucs/" + nameWithoutExtension(audio.input());
        StageValue<ODemucs> demucsOpts = stages.options(DEMUCS_OPTS, ODemucs.class);
        StageFile vocals = stages.file(demucsDir + "/vocals.wav", audio, demucsOpts);
        StageFile noVocals = stages.file(demucsDir + "/no_vocals.wav", audio, demucsOpts);
        if (isStage("Separating vocals and instrumental", vocals, noVocals)) {
            runner.runPythonExe(
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
            runner.runPythonScript(
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
            runner.runPythonScript(
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

        StageValue<OKaraoke> karaokeOpts = stages.options(KARAOKE_OPTS, OKaraoke.class);
        StageFile infoJson = new StageFile(info(audio.input()), true);
        StageFile scroll = stages.file("karaoke.ass", ass, infoJson, karaokeOpts);
        if (isStage("Creating karaoke subtitles", scroll)) {
            AssJoiner.join(ass.input(), infoJson.input(), scroll.output(), karaokeOpts.value());
        }

        StageFile video = new StageFile(video(audio.input()), true);
        StageFile karaoke = stages.file("karaoke.mp4", noVocals, scroll, video, karaokeOpts);
        if (isStage("Building karaoke video", karaoke)) {
            makeVideo(video.input(), noVocals.input(), scroll.input(), karaoke.output(), karaokeOpts.value());
        }

        long t1 = System.currentTimeMillis();
        log(String.format("Done in %s, result written to %s", CutRange.duration(t1 - t0), karaoke.input()));
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

    private static void addCodecs(List<String> args) {
        args.addAll(List.of(
            "-c:v", "libx264",
            "-c:a", "aac",
            "-b:a", "192k"
        ));
    }

    private void enlargeVideo(Path video, Path largeVideo, int width, int height) throws IOException, InterruptedException {
        String newSize = width + "x" + height;
        log("Enlarging small video to " + newSize + "...");
        List<String> ffmpeg = new ArrayList<>(List.of(
            "-i", video.toString(),
            "-y", "-stats",
            "-s", newSize
        ));
        addCodecs(ffmpeg);
        ffmpeg.add(largeVideo.toString());
        runner.runFFMPEG(ffmpeg);
    }

    private void makeVideo(Path video, Path noVocals, Path scroll, Path karaoke, OKaraoke opts) throws IOException, InterruptedException {
        List<String> videoInput;
        if (opts.video() && Files.exists(video)) {
            JSONObject obj = runner.runFFProbe(List.of(
                "-print_format", "json",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                video.toString()
            ));
            JSONObject stream = obj.getJSONArray("streams").getJSONObject(0);
            int width = stream.getInt("width");
            int height = stream.getInt("height");
            if (width < 1280) {
                int scale = (int) Math.ceil(1280.0 / width);
                int newWidth = scale * width;
                int newHeight = scale * height;
                Path largeVideo = video.resolveSibling(nameWithoutExtension(video) + ".big.mp4");
                enlargeVideo(video, largeVideo, newWidth, newHeight);
                videoInput = List.of(
                    "-i", largeVideo.toString()
                );
                log("Adding subtitles...");
            } else {
                videoInput = List.of(
                    "-i", video.toString()
                );
            }
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
            "-vf", "ass=" + escapeFilter(scroll.toString()),
            "-map", "0:v:0", // video from input 0
            "-map", "1:a:0", // audio from input 1
            "-shortest"
        ));
        addCodecs(ffmpeg);
        ffmpeg.add(karaoke.toString());
        runner.runFFMPEG(ffmpeg);
    }

    public void copyOptions(Path workDir) throws IOException {
        for (String opt : ALL_OPTS) {
            Path from = OptFile.path(rootDir, opt);
            if (!Files.exists(from))
                continue;
            Path to = OptFile.path(workDir, opt);
            if (Files.exists(to))
                continue;
            Files.copy(from, to);
        }
    }
}
