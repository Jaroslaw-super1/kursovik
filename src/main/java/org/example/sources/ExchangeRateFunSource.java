package org.example.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.core.AggregatedRecord;
import org.example.http.HttpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Источник курсов валют: запрашивает exchangerate.fun, инвертирует курсы
// (API отдаёт "сколько валюты за 1 ₽") и переводит золото из унции в грамм.
// Использован record для автоматического создания конструктора.
public record ExchangeRateFunSource(HttpClientWrapper http, ObjectMapper mapper) implements DataSource {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateFunSource.class);
    private static final String URL = "https://api.exchangerate.fun/latest?base=RUB";

    @Override
    public String getName() {
        return "exchangerate";
    }

    @Override
    public List<AggregatedRecord> fetch() {
        // 1. Создаем Map для хранения полей (сразу задаем дефолтные значения null)
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", null);
        fields.put("usdRub", null);
        fields.put("cnyRub", null);
        fields.put("xauRubPerGram", null);

        try {
            // 2. Делаем HTTP-запрос к API
            String body = http().get(URL); // Обращаемся к http через метод http()

            // 3. Парсим ответ JSON в дерево
            JsonNode root = mapper().readTree(body); // Обращаемся к mapper через метод mapper()

            // 4. Извлекаем timestamp (если он есть в ответе)
            if (root.has("timestamp")) {
                fields.put("timestamp", root.get("timestamp").asLong());
            }

            // 5. Достаем вложенный объект rates
            JsonNode rates = root.path("rates");
            if (rates.isObject()) {
                // 6. Инвертируем курсы USD и CNY, чтобы получить стоимость в рублях
                fields.put("usdRub", invert(rates, "USD"));
                fields.put("cnyRub", invert(rates, "CNY"));

                // 7. Пересчитываем золото: унция -> грамм
                if (rates.has("XAU")) {
                    fields.put("xauRubPerGram",
                            BigDecimal.valueOf(1.0 / rates.get("XAU").asDouble() / 31.1034768)
                                    .setScale(2, RoundingMode.HALF_UP));
                }
            }
        } catch (Exception e) {
            // 8. Если произошла ошибка (сеть, парсинг), логируем её.
            // Программа не падает, запись просто сохранится с null-полями.
            log.error("Failed to fetch rates from exchangerate.fun", e);
        }

        // 9. Возвращаем список с одной записью
        return List.of(new AggregatedRecord(getName(), Instant.now(), fields));
    }

    /**
     * Вспомогательный метод: "сколько единиц валюты за 1 ₽" → "сколько рублей за 1 единицу валюты"
     */
    private Object invert(JsonNode rates, String code) {
        // 1. Если такого кода нет в ответе — возвращаем null
        if (!rates.has(code)) return null;

        // 2. Достаем значение
        double v = rates.get(code).asDouble();

        // 3. Защита от деления на ноль
        if (v <= 0) return null;

        // 4. Инвертируем (1 / v) и округляем до 4 знаков
        return BigDecimal.valueOf(1.0 / v).setScale(4, RoundingMode.HALF_UP);
    }
}
