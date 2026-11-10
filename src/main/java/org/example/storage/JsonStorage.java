package org.example.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.core.AggregatedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Хранилище JSON: сохраняет список записей в файл (create/append) и читает его обратно.
// Использован record, так как класс содержит только ObjectMapper.
public record JsonStorage(ObjectMapper mapper) implements Storage {

    private static final Logger log = LoggerFactory.getLogger(JsonStorage.class);

    @Override
    public void save(List<AggregatedRecord> records, Path path, WriteMode mode) throws IOException {
        // 1. Определяем, какой список будем записывать
        List<AggregatedRecord> toWrite;
        if (mode == WriteMode.APPEND && Files.exists(path) && Files.size(path) > 0) {
            // 2. Если это APPEND, сначала читаем существующие записи, затем добавляем новые
            toWrite = new ArrayList<>(load(path));
            toWrite.addAll(records);
        } else {
            // 3. Если это CREATE (или файла нет), просто берем новые записи
            toWrite = new ArrayList<>(records);
        }

        // 4. Создаем родительские папки, если их нет
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        // 5. Записываем в файл (обращаемся к мапперу через метод mapper())
        mapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), toWrite);

        // 6. Логируем результат
        log.info("Saved {} records to {} (mode={}, format=json)", toWrite.size(), path, mode);
    }

    @Override
    public List<AggregatedRecord> load(Path path) throws IOException {
        // 1. Если файла нет, возвращаем пустой список и пишем WARN
        if (!Files.exists(path)) {
            log.warn("File {} does not exist, returning empty list", path);
            return new ArrayList<>();
        }

        // 2. Читаем JSON из файла и превращаем в список объектов
        return mapper().readValue(path.toFile(),
                new TypeReference<List<AggregatedRecord>>() {});
    }

    @Override
    public String readRaw(Path path) throws IOException {
        // 1. Если файла нет — выбрасываем ошибку
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }

        // 2. Читаем файл целиком как одну строку (UTF-8)
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
