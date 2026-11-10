package org.example.commands;

import org.example.core.Aggregator;
import org.example.core.AggregatedRecord;
import org.example.sources.DataSource;
import org.example.storage.Storage;
import org.example.storage.WriteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Сценарий опроса источников и сохранения в файл.
// Использован record для автоматического создания конструктора и полей.
public record AggregateCommand(Map<String, DataSource> sources, Map<String, Storage> storages) implements Command {

    private static final Logger log = LoggerFactory.getLogger(AggregateCommand.class);

    @Override
    public void execute(Map<String, String> params) throws IOException {
        // 1. Достаем параметры
        String sourcesStr = params.get("sources");
        String output = params.get("output");
        String modeStr = params.get("mode");
        String formatStr = params.get("format");

        // 2. Валидация обязательных параметров
        if (sourcesStr == null || output == null || modeStr == null || formatStr == null) {
            throw new IllegalArgumentException("Required params: --sources=... --output=... --format=... --mode=...");
        }

        List<String> sourceNames = Arrays.stream(sourcesStr.split(",")).map(String::trim).toList();
        WriteMode mode = WriteMode.valueOf(modeStr.toUpperCase());

        log.info("Start aggregation: sources={}, output={}, format={}, mode={}",
                sourceNames, output, formatStr, mode);

        // ВНИМАНИЕ: в record-ах обращение к полям идет через методы sources() и storages()
        Storage storage = storages().get(formatStr.toLowerCase());
        if (storage == null) {
            throw new IllegalArgumentException("Unknown format: " + formatStr);
        }

        // 4. Запускаем агрегацию
        Aggregator aggregator = new Aggregator(sources());
        List<AggregatedRecord> records = aggregator.aggregate(sourceNames);

        // 5. Сохраняем и выводим результат
        storage.save(records, Paths.get(output), mode);
        System.out.println("Done. Fetched " + records.size() + " records, saved to " + output);
    }
}
