package org.example.commands;

import org.example.core.AggregatedRecord;
import org.example.printers.RecordPrinter;
import org.example.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

// Сценарий чтения файла и вывода его на экран.
// Использован record для автоматического создания конструктора и полей.
public record ReadCommand(Map<String, Storage> storages, Map<String, RecordPrinter> printers) implements Command {
    private static final Logger log = LoggerFactory.getLogger(ReadCommand.class);

    @Override
    public void execute(Map<String, String> params) throws IOException {
        // 1. Получаем путь к файлу из аргументов
        String file = params.get("read");
        if (file == null || file.isBlank()) {
            throw new IllegalArgumentException("Parameter --read is required");
        }

        // 2. Определяем расширение файла и достаем нужное хранилище
        String ext = getExtension(file);
        Storage storage = storages().get(ext); // Обращаемся к мапе через метод storages()
        if (storage == null) {
            throw new IllegalArgumentException("Cannot detect storage for extension: ." + ext);
        }

        // 3. Определяем формат вывода (по умолчанию text)
        Path path = Paths.get(file);
        String printFormat = params.getOrDefault("print", "text").toLowerCase();

        // 4. Если запрошен сырой вывод (без парсинга), печатаем как есть и завершаем работу
        if ("raw".equals(printFormat)) {
            System.out.println(storage.readRaw(path));
            return;
        }

        // 5. Достаем нужный принтер из мапы
        RecordPrinter printer = printers().get(printFormat); // Обращаемся к мапе через метод printers()
        if (printer == null) {
            throw new IllegalArgumentException("Unknown print format: " + printFormat);
        }

        // 6. Загружаем данные из файла через хранилище
        List<AggregatedRecord> records = storage.load(path);

        // 7. Фильтруем по источнику (если передан --source)
        String sourceFilter = params.get("source");
        if (sourceFilter != null && !sourceFilter.isBlank()) {
            records = records.stream()
                    .filter(r -> sourceFilter.equalsIgnoreCase(r.source()))
                    .toList();
        }

        log.info("Read {} records from {}", records.size(), file);

        // 8. Отдаем данные принтеру для красивого вывода на экран
        printer.print(records);
    }

    // Вспомогательный метод для извлечения расширения из имени файла (например, "csv" из "data.csv")
    private String getExtension(String file) {
        int dot = file.lastIndexOf('.');
        if (dot == -1 || dot == file.length() - 1) {
            throw new IllegalArgumentException("Cannot determine file extension for: " + file);
        }
        return file.substring(dot + 1).toLowerCase();
    }
}
