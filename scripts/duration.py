import sys
import wave


def duration(no_vocals_path, duration_path):
    with wave.open(no_vocals_path, "r") as f:
        frames = f.getnframes()
        rate = f.getframerate()
        duration = frames / float(rate)

    with open(duration_path, "w", encoding="utf-8") as f:
        print(f"{duration:2.2f}", file=f)


if __name__ == '__main__':
    duration(sys.argv[1], sys.argv[2])
