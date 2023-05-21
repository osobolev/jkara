package jkara.opts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class OptFile<O extends Record> {

    public final List<Path> files;
    public final O value;

    private OptFile(List<Path> files, O value) {
        this.files = files;
        this.value = value;
    }

    public static <O extends Record> OptFile<O> read(Class<O> cls, Path... files) throws IOException {
        RecordComponent[] components = cls.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] values = new Object[components.length];
        try {
            O defValue = cls.getConstructor().newInstance();
            for (int i = 0; i < components.length; i++) {
                RecordComponent component = components[i];
                types[i] = component.getType();
                values[i] = component.getAccessor().invoke(defValue);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        Properties props = new Properties();
        List<Path> fileList = new ArrayList<>();
        for (Path file : files) {
            if (!Files.exists(file))
                continue;
            fileList.add(file);
            try (BufferedReader rdr = Files.newBufferedReader(file)) {
                props.load(rdr);
            }
        }
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            String strValue = props.getProperty(component.getName());
            if (strValue != null) {
                Class<?> type = component.getType();
                Object newValue;
                if (type == String.class) {
                    newValue = strValue;
                } else if (type == Boolean.class || type == boolean.class) {
                    newValue = Boolean.parseBoolean(strValue);
                } else if (type == Integer.class || type == int.class) {
                    try {
                        newValue = Integer.parseInt(strValue);
                    } catch (NumberFormatException nfex) {
                        throw new IllegalStateException(nfex);
                    }
                } else if (type == Double.class || type == double.class) {
                    try {
                        newValue = Double.parseDouble(strValue);
                    } catch (NumberFormatException nfex) {
                        throw new IllegalStateException(nfex);
                    }
                } else {
                    throw new IllegalStateException("Unsupported field type: " + type);
                }
                values[i] = newValue;
            }
        }
        try {
            O value = cls.getConstructor(types).newInstance(values);
            return new OptFile<>(fileList, value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static <O extends Record> OptFile<O> read(Path rootDir, Path dir, String name, Class<O> cls) throws IOException {
        String fileName = name + ".properties";
        Path[] files = {rootDir.resolve(fileName), dir.resolve(fileName)};
        return read(cls, files);
    }

    public static <O extends Record> void write(Path file, O value) throws IOException {
        RecordComponent[] components = value.getClass().getRecordComponents();
        Properties props = new Properties();
        try {
            for (RecordComponent component : components) {
                Object fieldValue = component.getAccessor().invoke(value);
                if (fieldValue != null) {
                    props.put(component.getName(), fieldValue.toString());
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            props.store(w, null);
        }
    }
}
