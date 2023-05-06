import json
import logging
import os
import sys

logging.getLogger("speechbrain.utils.torch_audio_backend").setLevel(logging.ERROR)
logging.getLogger("speechbrain.utils.train_logger").setLevel(logging.ERROR)

import torch
import whisperx


def align(audio, text_json, aligned_json):
    with open(text_json, "r", encoding="utf-8") as json_file:
        data = json.load(json_file)

    device = "cuda" if torch.cuda.is_available() else "cpu"
    language = data["language"]
    align_model, align_metadata = whisperx.load_align_model(language, device, model_name="WAV2VEC2_ASR_LARGE_LV60K_960H")
    aligned = whisperx.align(data["segments"], align_model, align_metadata, audio, device, extend_duration=2.0)
    
    for segment in aligned["segments"]:
        char_segments = []
        for cidx, crow in segment["char-segments"].iterrows():
            char_segments.append(crow.to_dict())
        segment["char-segments"] = char_segments
        word_segments = []
        for cidx, crow in segment["word-segments"].iterrows():
            word_segments.append(crow.to_dict())
        segment["word-segments"] = word_segments
    with open(aligned_json, "w", encoding="utf-8") as js_file:
        json.dump(aligned, js_file, sort_keys=True, indent=4, allow_nan=True, ensure_ascii=False)


if __name__ == '__main__':
    align(sys.argv[1], sys.argv[2], sys.argv[3])
