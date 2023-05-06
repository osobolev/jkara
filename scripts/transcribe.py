import json
import sys

import faster_whisper


def transcribe(audio, json_path):
    model = faster_whisper.WhisperModel("large-v2", compute_type="float32")
    language = "en" # todo!!!
    segments, info = model.transcribe(audio, language=language, vad_filter=True)
    
    out_segments = []
    for segment in segments:
        out_segments.append({ "start": segment.start, "end": segment.end, "text": segment.text })
    js_data = { "segments": out_segments, "language": info.language }

    with open(json_path, "w") as js_file:
        json.dump(js_data, js_file, sort_keys=True, indent=4, allow_nan=True)


if __name__ == '__main__':
    transcribe(sys.argv[1], sys.argv[2])
