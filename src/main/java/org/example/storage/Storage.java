package org.example.storage;

import org.example.core.AggregatedRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Storage {
    void save(List<AggregatedRecord> records, Path path, WriteMode mode) throws IOException;
    List<AggregatedRecord> load(Path path) throws IOException;

    // Отдает содержимое файла как есть (для вывода в режиме --print=raw)
    String readRaw(Path path) throws IOException;
}