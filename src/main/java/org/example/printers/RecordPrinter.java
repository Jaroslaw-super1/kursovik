package org.example.printers;

import org.example.core.AggregatedRecord;
import java.util.List;

// Контракт для форматирования и вывода данных на экран.
public interface RecordPrinter {
    void print(List<AggregatedRecord> records);
}
