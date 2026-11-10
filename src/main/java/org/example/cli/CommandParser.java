package org.example.cli;

import java.util.HashMap;
import java.util.Map;

// Разбирает аргументы командной строки в Map.
// Пример: "--read=data.json --print=json" -> {read: "data.json", print: "json"}
public class CommandParser {
    public static Map<String, String> parse(String[] args) {
        Map<String, String> result = new HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--")) continue;
            int eq = arg.indexOf('=');
            if (eq < 0) {
                result.put(arg.substring(2), ""); // флаг без значения (например, --json)
            } else {
                result.put(arg.substring(2, eq), arg.substring(eq + 1));
            }
        }
        return result;
    }
}
