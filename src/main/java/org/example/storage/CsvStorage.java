package org.example.storage;

import org.example.core.AggregatedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Хранилище CSV в long-формате: одна строка = одно поле записи.
// Колонки: source,fetchedAt,field,value. Работает с любыми наборами полей.
// Использован record без полей (пустые скобки), так как класс не хранит состояние.
public record CsvStorage() implements Storage {
    private static final Logger log = LoggerFactory.getLogger(CsvStorage.class);
    private static final String HEADER = "source,fetchedAt,field,value";

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

        // 5. Открываем файл на запись и пишем заголовок
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();

            // 6. Проходим по каждой записи и пишем её поля в long-format
            for (AggregatedRecord r : toWrite) {
                String fetchedAt = r.fetchedAt().toString();
                for (Map.Entry<String, Object> e : r.fields().entrySet()) {
                    String value = e.getValue() == null ? "" : String.valueOf(e.getValue());
                    w.write(escape(r.source()));
                    w.write(',');
                    w.write(escape(fetchedAt));
                    w.write(',');
                    w.write(escape(e.getKey()));
                    w.write(',');
                    w.write(escape(value));
                    w.newLine();
                }
            }
        }
        // 7. Логируем успешное сохранение
        log.info("Saved {} records to {} (mode={}, format=csv)", toWrite.size(), path, mode);
    }

    @Override
    public List<AggregatedRecord> load(Path path) throws IOException {
        // 1. Если файла нет, возвращаем пустой список и пишем WARN
        if (!Files.exists(path)) {
            log.warn("File {} does not exist, returning empty list", path);
            return new ArrayList<>();
        }

        // 2. Подготавливаем переменные для сборки текущей записи
        List<AggregatedRecord> records = new ArrayList<>();
        String currentSource = null;
        Instant currentTime = null;
        Map<String, Object> currentFields = new LinkedHashMap<>();

        // 3. Открываем файл на чтение и пропускаем первую строку (заголовок)
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = r.readLine();
            if (line == null) return records;

            // 4. Читаем файл построчно
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cells = parseLine(line);
                if (cells.size() < 4) continue;

                // 5. Разбираем строку на колонки
                String source = cells.get(0);
                Instant fetchedAt = Instant.parse(cells.get(1));
                String field = cells.get(2);
                Object value = parseValue(cells.get(3));

                // 6. Если сменился source или fetchedAt, значит началась новая запись.
                // Сохраняем предыдущую и очищаем мапу для полей.
                if (currentSource != null
                        && (!currentSource.equals(source) || !currentTime.equals(fetchedAt))) {
                    records.add(new AggregatedRecord(currentSource, currentTime, currentFields));
                    currentFields = new LinkedHashMap<>();
                }

                // 7. Запоминаем текущие значения и добавляем поле в мапу
                currentSource = source;
                currentTime = fetchedAt;
                currentFields.put(field, value);
            }
        }

        // 8. После цикла не забываем сохранить последнюю запись
        if (currentSource != null) {
            records.add(new AggregatedRecord(currentSource, currentTime, currentFields));
        }

        // 9. Возвращаем готовый список
        return records;
    }

    @Override
    public String readRaw(Path path) throws IOException {
        // 1. Проверяем существование файла
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        // 2. Читаем файл целиком как строку
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    // Экранирование по RFC 4180: оборачиваем в кавычки, если есть запятая или кавычки
    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // Парсер строки CSV с поддержкой quoted-полей (кавычек)
    private static List<String> parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { result.add(cur.toString()); cur = new StringBuilder(); }
                else cur.append(c);
            }
        }
        result.add(cur.toString());
        return result;
    }

    // Парсинг значений: "null" или пустота -> null; целое -> Long; дробное -> Double; иначе -> String
    private static Object parseValue(String s) {
        if (s == null || s.isEmpty() || "null".equals(s)) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        return s;
    }
}
