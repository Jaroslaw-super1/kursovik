package org.example.sources;

import org.example.core.AggregatedRecord;

import java.util.List;

// Контракт любого источника данных: имя для реестра и метод fetch(),
// возвращающий список записей в единой модели.
// Реализуется: MoexCurrencySource, SevenTimerSource, ExchangeRateFunSource.
// Используется: SourceRegistry (хранит), Aggregator (вызывает fetch()).
public interface DataSource {
    String getName();
    List<AggregatedRecord> fetch();
}
