package org.example.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.core.AggregatedRecord;
import org.example.http.HttpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Источник погоды: запрашивает 7Timer! по координатам СПб и берёт первый
// элемент dataseries (прогноз на +N часов от init) — температуру, ветер, облачность.
// Использован record для автоматического создания конструктора и полей.
public record SevenTimerSource(HttpClientWrapper http, ObjectMapper mapper) implements DataSource {

    private static final Logger log = LoggerFactory.getLogger(SevenTimerSource.class);

    private static final String URL =
            "https://www.7timer.info/bin/api.pl?lon=30.3351&lat=59.9343&product=civil&output=json";

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public List<AggregatedRecord> fetch() {
        // 1. Создаем Map для хранения извлеченных полей (используем LinkedHashMap для сохранения порядка)
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("city", "Saint Petersburg");
        fields.put("init", null);
        fields.put("timepoint", null);
        fields.put("temperature", null);
        fields.put("windSpeed", null);
        fields.put("windDirection", null);
        fields.put("cloudCover", null);
        fields.put("precipitation", null);

        try {
            // 2. Выполняем HTTP-запрос с помощью обертки http()
            log.info("Calling 7Timer: {}", URL);
            String body = http().get(URL);
            log.info("Got {} bytes from 7Timer", body.length());

            // 3. Парсим ответ от API в JSON-дерево с помощью mapper()
            JsonNode root = mapper().readTree(body);

            // 4. Извлекаем время инициализации прогноза (если оно есть)
            if (root.hasNonNull("init")) {
                fields.put("init", root.get("init").asText());
            }

            // 5. Достаем массив с прогнозами
            JsonNode dataSeries = root.path("dataseries");

            // 6. Если массив не пустой, берем первый элемент (ближайший прогноз)
            if (dataSeries.isArray() && !dataSeries.isEmpty()) {
                JsonNode first = dataSeries.get(0);

                // 7. Извлекаем конкретные значения (температуру, ветер и т.д.)
                putNum(fields, "timepoint",     first.path("timepoint"));
                putNum(fields, "temperature",   first.path("temp2m"));
                putNum(fields, "windSpeed",     first.path("wind10m").path("speed"));
                putText(fields, "windDirection", first.path("wind10m").path("direction"));
                putNum(fields, "cloudCover",    first.path("cloudcover"));
                putText(fields, "precipitation", first.path("prec_type"));
            }
        } catch (Exception e) {
            // 8. При любой сетевой или ошибке парсинга пишем лог (данные вернутся с null-полями)
            log.error("Failed to fetch weather from 7Timer", e);
        }

        // 9. Возвращаем готовую запись (используем getName() и текущее время)
        return List.of(new AggregatedRecord(getName(), Instant.now(), fields));
    }

    // Вспомогательный метод для безопасного парсинга чисел
    private void putNum(Map<String, Object> f, String k, JsonNode n) {
        if (n.isNumber()) {
            f.put(k, n.numberValue());
        } else if (n.isTextual() && !n.asText().isBlank()) {
            try { f.put(k, Double.parseDouble(n.asText())); }
            catch (NumberFormatException ignored) {}
        }
    }

    // Вспомогательный метод для безопасного парсинга строк
    private void putText(Map<String, Object> f, String k, JsonNode n) {
        if (n.isTextual()) f.put(k, n.asText());
    }
}
