package org.example.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.commands.AggregateCommand;
import org.example.commands.Command;
import org.example.commands.ReadCommand;
import org.example.http.HttpClientWrapper;
import org.example.printers.CsvRecordPrinter;
import org.example.printers.JsonRecordPrinter;
import org.example.printers.RecordPrinter;
import org.example.printers.TextRecordPrinter;
import org.example.sources.DataSource;
import org.example.sources.ExchangeRateFunSource;
import org.example.sources.MoexCurrencySource;
import org.example.sources.SevenTimerSource;
import org.example.storage.CsvStorage;
import org.example.storage.JsonStorage;
import org.example.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Точка входа. Собирает все зависимости и вызывает нужную команду.
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 1. Создаем общие зависимости
        ObjectMapper mapper = createMapper();
        HttpClientWrapper http = new HttpClientWrapper();

        // 2. Регистрируем все ресурсы (источники API)
        Map<String, DataSource> sources = new HashMap<>();
        sources.put("moex", new MoexCurrencySource(http, mapper));
        sources.put("weather", new SevenTimerSource(http, mapper));
        sources.put("exchangerate", new ExchangeRateFunSource(http, mapper));

        // 3. Регистрируем хранилища (по расширению файла)
        Map<String, Storage> storages = new HashMap<>();
        storages.put("json", new JsonStorage(mapper));
        storages.put("csv", new CsvStorage());

        // 4. Регистрируем принтеры (как выводить на экран)
        Map<String, RecordPrinter> printers = new HashMap<>();
        printers.put("text", new TextRecordPrinter());
        printers.put("csv", new CsvRecordPrinter());
        printers.put("json", new JsonRecordPrinter(mapper));

        // 5. Разбираем аргументы
        Map<String, String> params = CommandParser.parse(args);

        // 6. Выбираем команду и выполняем
        Command command;
        if (params.containsKey("read")) {
            command = new ReadCommand(storages, printers);
        } else {
            command = new AggregateCommand(sources, storages);
        }

        try {
            command.execute(params);
        } catch (IOException e) {
            log.error("I/O error: {}", e.getMessage(), e);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument: {}", e.getMessage(), e);
            System.exit(2);
        }
    }

    // Создает и настраивает ObjectMapper для всего приложения
    private static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }
}
