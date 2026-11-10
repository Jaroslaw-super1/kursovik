package org.example.core;

import java.time.Instant;
import java.util.Map;

// Единая модель записи: source — имя источника (moex/weather/exchangerate),
// fetchedAt — когда получены данные, fields — все извлечённые поля в Map.
// Используется: всеми sources (создают), JsonStorage (сериализует), RecordFormatter (читает).
public record AggregatedRecord(
        String source,
        Instant fetchedAt,
        Map<String, Object> fields
) {}
