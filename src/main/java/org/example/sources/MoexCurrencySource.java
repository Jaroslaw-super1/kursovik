package org.example.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.core.AggregatedRecord;
import org.example.http.HttpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Источник данных MOEX ISS: запрашивает текущие торги по USD, CNY и золоту,
// вытаскивает LAST/OPEN/BID/OFFER/объёмы и считает изменение за день.
// Использован record для автоматического создания конструктора и полей.
public record MoexCurrencySource(HttpClientWrapper http, ObjectMapper mapper) implements DataSource {

    private static final Logger log = LoggerFactory.getLogger(MoexCurrencySource.class);

    private static final String BASE = "https://iss.moex.com/iss/engines/currency/markets/selt/securities/";
    private static final List<String> TICKERS = List.of("USD000UTSTOM", "CNYRUB_TOM", "GLDRUB_TOM");
    private static final Set<String> FIELDS = Set.of(
            "SECID", "LAST", "OPEN", "BID", "OFFER",
            "BIDDEPTHT", "OFFERDEPTHT", "VOLTODAY", "VALTODAY",
            "NUMTRADES", "UPDATETIME"
    );

    @Override
    public String getName() {
        return "moex";
    }

    @Override
    public List<AggregatedRecord> fetch() {
        // 1. Создаем список для сбора результатов по всем тикерам
        List<AggregatedRecord> result = new ArrayList<>();

        // 2. Проходим по списку тикеров (доллар, юань, золото)
        for (String ticker : TICKERS) {
            result.add(fetchOne(ticker));
        }

        // 3. Возвращаем список из 3-х записей
        return result;
    }

    private AggregatedRecord fetchOne(String ticker) {
        // 1. Создаем Map для полей и заполняем ключами со значением null
        Map<String, Object> fields = new LinkedHashMap<>();
        for (String f : FIELDS) fields.put(f, null);
        fields.put("ticker", ticker);
        fields.put("change", null);

        try {
            // 2. Формируем URL и делаем HTTP-запрос (обращаемся к полю через http())
            String url = BASE + ticker + ".json?iss.meta=off";
            String body = http().get(url);

            // 3. Парсим ответ в JSON-дерево (обращаемся к полю через mapper())
            JsonNode root = mapper().readTree(body);

            // 4. Вытаскиваем данные из блоков securities и marketdata
            mergeBlock(root.path("securities"), fields);
            mergeBlock(root.path("marketdata"), fields);

            // 5. Считаем изменение цены за день (LAST - OPEN)
            Object last = fields.get("LAST");
            Object open = fields.get("OPEN");
            if (last instanceof Number && open instanceof Number) {
                double change = ((Number) last).doubleValue() - ((Number) open).doubleValue();
                fields.put("change", change);
            }
        } catch (Exception e) {
            // 6. Если произошла ошибка (сеть, парсинг) — логируем её, запись сохранится с null-полями
            log.error("Failed to fetch MOEX {} ", ticker, e);
        }

        // 7. Возвращаем готовую модель записи
        return new AggregatedRecord(getName(), Instant.now(), fields);
    }

    private void mergeBlock(JsonNode block, Map<String, Object> fields) {
        // 1. Достаем колонки и строки из блока JSON
        JsonNode columns = block.path("columns");
        JsonNode data = block.path("data");
        if (!columns.isArray() || !data.isArray() || data.isEmpty()) return;

        JsonNode row = data.get(0);

        // 2. Проходим по колонкам и сопоставляем их с нужными нам полями (FIELDS)
        for (int i = 0; i < columns.size() && i < row.size(); i++) {
            String col = columns.get(i).asText();
            if (!FIELDS.contains(col)) continue;

            // 3. Записываем значение в Map, если оно не null
            JsonNode val = row.get(i);
            if (val.isNull() || val.isMissingNode()) {
                // оставляем null
            } else if (val.isNumber()) {
                fields.put(col, val.numberValue());
            } else {
                fields.put(col, val.asText());
            }
        }
    }
}
