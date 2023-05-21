package jkara;

import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record OInput(
    String url,
    String audio,
    String language,
    String text
) {

    public OInput() {
        this(null, null, null, null);
    }

    private static String path(Path dir, Path file) {
        try {
            Path relPath = dir.relativize(file);
            return relPath.toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return file.toAbsolutePath().toString();
        }
    }

    static OInput create(Path dir, String url, Path audio, String language, Path text) {
        return new OInput(
            url, path(dir, audio), language, path(dir, text)
        );
    }

    static List<String> diff(OInput i1, OInput i2) {
        RecordComponent[] components = OInput.class.getRecordComponents();
        try {
            List<String> diff = new ArrayList<>();
            for (RecordComponent component : components) {
                Object v1 = component.getAccessor().invoke(i1);
                Object v2 = component.getAccessor().invoke(i2);
                if (!Objects.equals(v1, v2)) {
                    diff.add(component.getName());
                }
            }
            return diff;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
