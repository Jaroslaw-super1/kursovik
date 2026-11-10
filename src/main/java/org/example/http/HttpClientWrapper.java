package org.example.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// Обёртка над HTTP GET: выполняет запрос, читает тело ответа как строку
// и кидает IOException при статусе != 200. Единственное место с сетевыми вызовами.
// Используется: все источники (MoexCurrencySource, SevenTimerSource, ExchangeRateFunSource).
public class HttpClientWrapper {

    public String get(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (aggregator-coursework)");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
        }
        conn.disconnect();

        if (code != 200) {
            throw new IOException("HTTP " + code + " for " + urlStr);
        }
        return sb.toString();
    }
}
