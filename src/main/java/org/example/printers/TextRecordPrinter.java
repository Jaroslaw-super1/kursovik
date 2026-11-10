package org.example.printers;

import org.example.core.AggregatedRecord;
import java.util.List;
import java.util.Map;

public class TextRecordPrinter implements RecordPrinter {

    @Override
    public void print(List<AggregatedRecord> records) {
        for (AggregatedRecord r : records) {
            String formatted = switch (r.source()) {
                case "moex"         -> formatMoex(r);
                case "exchangerate" -> formatExchangeRate(r);
                case "weather"      -> formatWeather(r);
                default             -> formatGeneric(r);
            };
            System.out.println(formatted);
        }
    }

    private String formatMoex(AggregatedRecord r) {
        Map<String, Object> f = r.fields();
        String name = friendlyName(str(f.get("ticker")));
        return String.format("%s: открытие %s, текущая %s, изменение %s",
                name, fmt(f.get("OPEN")), fmt(f.get("LAST")), fmtSigned(f.get("change")));
    }

    private String formatExchangeRate(AggregatedRecord r) {
        Map<String, Object> f = r.fields();
        return "Курсы валют (exchangerate):\n"
                + "  Доллар: " + fmt(f.get("usdRub")) + " ₽\n"
                + "  Юань: " + fmt(f.get("cnyRub")) + " ₽\n"
                + "  Золото в рублях за грамм: " + fmt(f.get("xauRubPerGram")) + " ₽";
    }

    private String formatWeather(AggregatedRecord r) {
        Map<String, Object> f = r.fields();
        if (f.get("temperature") == null) return "Погода (СПб): данные недоступны";
        return String.format("Погода (СПб) через 3 часа: %s°C, ветер %s км/ч (%s), облачность %s/9, осадки: %s",
                fmt(f.get("temperature")), fmt(f.get("windSpeed")), str(f.get("windDirection")),
                fmt(f.get("cloudCover")), str(f.get("precipitation")));
    }

    private String formatGeneric(AggregatedRecord r) {
        return r.source() + ": " + r.fields();
    }

    private String friendlyName(String ticker) {
        if (ticker == null) return "?";
        return switch (ticker) {
            case "USD000UTSTOM" -> "Доллар (MOEX)";
            case "CNYRUB_TOM"   -> "Юань (MOEX)";
            case "GLDRUB_TOM"   -> "Золото (MOEX)";
            default             -> ticker;
        };
    }

    private String fmt(Object o) {
        if (o == null) return "—";
        if (o instanceof Number n) {
            double d = n.doubleValue();
            return Math.abs(d) >= 100 ? String.format("%.2f", d) : String.format("%.4f", d);
        }
        return o.toString();
    }

    private String fmtSigned(Object o) {
        if (o == null) return "—";
        if (o instanceof Number n) {
            double d = n.doubleValue();
            return (d >= 0 ? "+" : "") + String.format("%.4f", d);
        }
        return o.toString();
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
