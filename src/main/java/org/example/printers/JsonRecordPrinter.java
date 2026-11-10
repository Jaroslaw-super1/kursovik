package org.example.printers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.core.AggregatedRecord;
import java.util.List;

public class JsonRecordPrinter implements RecordPrinter {
    private final ObjectMapper mapper;

    public JsonRecordPrinter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void print(List<AggregatedRecord> records) {
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(records);
            System.out.println(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to format JSON", e);
        }
    }
}
