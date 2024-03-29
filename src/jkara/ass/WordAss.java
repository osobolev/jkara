package jkara.ass;

import jkara.sync.WordLine;
import jkara.sync.TimedWord;
import jkara.util.OutputFactory;
import jkara.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class WordAss {

    public static void writeAss(List<WordLine> lines, OutputFactory factory) throws IOException {
        List<String> assLines = new ArrayList<>();
        for (WordLine line : lines) {
            StringBuilder buf = new StringBuilder();
            for (TimedWord word : line.words()) {
                Util.appendK(buf, "K", word.time());
                buf.append(word.text());
            }
            assLines.add(AssWriter.assLine(line.start(), line.end(), buf.toString()));
        }
        AssWriter.write(factory, assLines.stream());
    }
}
