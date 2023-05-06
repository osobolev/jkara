package jkara.sync;

import java.nio.file.Path;

public final class SyncException extends Exception {

    public final Path fastJson;

    public SyncException(String message, Path fastJson) {
        super(message);
        this.fastJson = fastJson;
    }
}
