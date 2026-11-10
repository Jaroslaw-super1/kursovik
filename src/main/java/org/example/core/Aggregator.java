package org.example.core;

import org.example.sources.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Оркестратор: получает Map доступных источников и имена тех, которые надо опросить.
// Вызывает fetch() у нужных и собирает результат в один список.
public record Aggregator(Map<String, DataSource> availableSources) {

    private static final Logger log = LoggerFactory.getLogger(Aggregator.class);

    public List<AggregatedRecord> aggregate(List<String> sourceNames) {
        // 1. Создаем список для сбора всех записей
        List<AggregatedRecord> result = new ArrayList<>();

        // 2. Проходим по списку имен источников, которые запросил пользователь
        for (String name : sourceNames) {

            // 3. Ищем нужный источник в нашей Map (обращаемся к полю через метод availableSources())
            DataSource source = availableSources().get(name);

            // 4. Если источника с таким именем нет — пишем WARN и идем к следующему
            if (source == null) {
                log.warn("Unknown source: '{}'", name);
                continue;
            }

            // 5. Логируем начало работы с конкретным API
            log.info("Fetching source: {}", name);

            try {
                // 6. Запрашиваем данные у источника (HTTP-запрос и парсинг происходят внутри fetch)
                List<AggregatedRecord> records = source.fetch();

                // 7. Логируем успех и добавляем полученные записи в общий список
                log.info("Source {} returned {} records", name, records.size());
                result.addAll(records);

            } catch (RuntimeException e) {
                // 8. Если источник упал с ошибкой — логируем её, но продолжаем опрос остальных
                log.error("Source {} failed unexpectedly", name, e);
            }
        }

        // 9. Возвращаем итоговый объединенный список
        return result;
    }
}