package jkara.pipe;

import jkara.ass.AssSync;
import jkara.sync.TextSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KaraPipe {

    /**
     * Pipeline:
     * vocals.wav -> fast_whisper -> fast.json (semi-accurate transcription)
     * text.txt + fast.json -> TextSync -> text.json (real lyrics with timestamps from prev step)
     * text.json -> whisperx -> aligned.json (character-level timestamps for real lyrics)
     * aligned.json + text.txt -> AssSync -> ass file
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // todo: run youtube-download
        // todo: run demucs
        // todo: create karaoke video

        Path audio = Path.of("C:\\home\\projects\\my\\kara\\work\\separated\\htdemucs\\PjdEDOIai8Y\\vocals.wav");
        Path text = Path.of("C:\\home\\projects\\my\\kara\\work\\text.txt");

        ProcRunner runner = new ProcRunner(Path.of("ffmpeg").toAbsolutePath());

        Path fastJson = Path.of("C:\\home\\projects\\my\\kara\\work\\_fast.json");
        if (!Files.exists(fastJson)) {
            runner.runPython("scripts/transcribe.py", audio.toString(), fastJson.toString());
        }
        Path textJson = Path.of("C:\\home\\projects\\my\\kara\\work\\_text.json");
        if (!Files.exists(textJson)) {
            TextSync.sync(text, fastJson, () -> Files.newBufferedWriter(textJson));
        }
        Path alignedJson = Path.of("C:\\home\\projects\\my\\kara\\work\\_aligned.json");
        if (!Files.exists(alignedJson)) {
            runner.runPython("scripts/align.py", audio.toString(), textJson.toString(), alignedJson.toString());
        }
        AssSync.sync(text, alignedJson);
    }
}
