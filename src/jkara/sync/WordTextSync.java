package jkara.sync;

import jkara.util.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class WordTextSync {

    private static boolean isWord(char ch) {
        return Character.isLetterOrDigit(ch);
    }

    private record Word(String word, String extra)
    {}

    private record WordWithTS(double start, double end, String word, String extra)
    {}

    private static final class WordParser {

        private final String text;
        private int i = 0;

        private final List<Word> words = new ArrayList<>();

        WordParser(String text) {
            this.text = text;
        }

        private void skipSpaces() {
            while (i < text.length()) {
                char ch = text.charAt(i);
                if (isWord(ch))
                    break;
                i++;
            }
        }

        private void skipWord() {
            while (i < text.length()) {
                char ch = text.charAt(i);
                if (!isWord(ch))
                    break;
                i++;
            }
        }

        List<Word> parse() {
            skipSpaces();
            while (i < text.length()) {
                int wordStart = i;
                skipWord();
                int wordEnd = i;
                skipSpaces();
                int end = i;
                words.add(new Word(text.substring(wordStart, wordEnd), text.substring(wordEnd, end)));
            }
            return words;
        }
    }

    private record SrcSegment(int segment, int origFrom, int origTo)
    {}

    public static List<WordLine> sync(Path text, Path fastJson) throws IOException, SyncException {
        String realText = Util.readLyrics(text);
        Map<Integer, CWS> origMap = new HashMap<>();
        List<CWS> real = new ArrayList<>();
        Normalizer.append(real, realText, null, origMap::put);
        Normalizer.finish(real);
        FastResult fast = FastResult.read(fastJson);

        TextSync.sync(text, real, fastJson, fast);

        int[] segments = new int[realText.length()];
        Arrays.fill(segments, -1);
        for (int i = 0; i < realText.length(); i++) {
            CWS cws = origMap.get(i);
            Integer segment = cws == null ? null : cws.segment;
            if (segment != null) {
                segments[i] = segment.intValue();
            }
        }
        {
            int prevSegment = 0;
            for (int i = 0; i < segments.length; i++) {
                int segment = segments[i];
                if (segment < 0) {
                    segments[i] = prevSegment;
                } else {
                    prevSegment = segment;
                }
            }
        }
        List<SrcSegment> srcSegments = new ArrayList<>();
        {
            int prevSegmentStart = -1;
            for (int i = 0; i < segments.length; i++) {
                int segment = segments[i];
                if (prevSegmentStart < 0 || segment != segments[prevSegmentStart]) {
                    if (prevSegmentStart >= 0) {
                        srcSegments.add(new SrcSegment(segments[prevSegmentStart], prevSegmentStart, i));
                    }
                    prevSegmentStart = i;
                }
            }
            if (prevSegmentStart >= 0) {
                srcSegments.add(new SrcSegment(segments[prevSegmentStart], prevSegmentStart, segments.length));
            }
        }
        List<WordWithTS> words = new ArrayList<>();
        for (SrcSegment srcSegment : srcSegments) {
            Segment segment = fast.segments.get(srcSegment.segment());
            double segmentLen = segment.end() - segment.start();
            String src = realText.substring(srcSegment.origFrom(), srcSegment.origTo());
            List<Word> segmentWords = new WordParser(src).parse();
            int sumWordsLen = segmentWords.stream().mapToInt(w -> w.word().length()).sum();
            if (sumWordsLen <= 0)
                continue;
            double start = segment.start();
            for (Word word : segmentWords) {
                double time = (double) word.word().length() / sumWordsLen * segmentLen;
                words.add(new WordWithTS(start, start + time, word.word(), word.extra()));
                start += time;
            }
        }
        List<List<WordWithTS>> lines = new ArrayList<>();
        lines.add(new ArrayList<>());
        for (WordWithTS word : words) {
            List<WordWithTS> currentLine = lines.get(lines.size() - 1);
            if (word.extra().indexOf('\n') >= 0) {
                currentLine.add(new WordWithTS(word.start(), word.end(), word.word(), ""));
                lines.add(new ArrayList<>());
            } else {
                currentLine.add(word);
            }
        }
        return lines.stream().filter(line -> !line.isEmpty()).map(line -> {
            double start = line.get(0).start();
            double end = line.get(line.size() - 1).end();
            List<TimedWord> twords = line.stream().map(w -> new TimedWord(w.end() - w.start(), w.word() + w.extra())).toList();
            return new WordLine(start, end, twords);
        }).toList();
    }
}
