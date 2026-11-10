package org.example.printers;

import org.example.core.AggregatedRecord;
import java.util.List;
import java.util.Map;

public class CsvRecordPrinter implements RecordPrinter {

    @Override
    public void print(List<AggregatedRecord> records) {
        System.out.println("source,fetchedAt,field,value");
        for (AggregatedRecord r : records) {
            String fetchedAt = r.fetchedAt().toString();
            for (Map.Entry<String, Object> e : r.fields().entrySet()) {
                String value = e.getValue() == null ? "" : String.valueOf(e.getValue());
                // Простое экранирование на случай, если в значении есть запятые
                if (value.contains(",")) value = "\"" + value + "\"";
                System.out.printf("%s,%s,%s,%s%n", r.source(), fetchedAt, e.getKey(), value);
            }
        }
    }
}
