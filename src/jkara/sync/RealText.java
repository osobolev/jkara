package jkara.sync;

import jkara.util.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class RealText {

    final List<CWS> list;

    private RealText(List<CWS> list) {
        this.list = list;
    }

    static RealText read(Path file) throws IOException {
        String str = Util.readLyrics(file);
        List<CWS> buf = new ArrayList<>();
        Normalizer.append(buf, str, null);
        Normalizer.finish(buf);
        return new RealText(buf);
    }
}
