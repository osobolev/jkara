import json
import logging
import sys

import faster_whisper


logging.basicConfig()


def transcribe(audio, json_path, language):
    model = faster_whisper.WhisperModel("large-v2", compute_type="float32")
    model.logger.setLevel(logging.DEBUG)
    segments, info = model.transcribe(audio, language=language, vad_filter=True)
    
    out_segments = []
    for segment in segments:
        out_segments.append({ "start": segment.start, "end": segment.end, "text": segment.text })
    js_data = { "segments": out_segments, "language": info.language }

    with open(json_path, "w", encoding="utf-8") as js_file:
        json.dump(js_data, js_file, sort_keys=True, indent=4, allow_nan=True, ensure_ascii=False)


if __name__ == '__main__':
    if len(sys.argv) < 4:
        language = None
    else:
        lang_str = sys.argv[3]
        language = None if len(lang_str) < 2 else lang_str
    transcribe(sys.argv[1], sys.argv[2], language)
