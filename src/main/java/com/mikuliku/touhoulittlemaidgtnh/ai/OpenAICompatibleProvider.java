package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OpenAICompatibleProvider implements AIProvider {
    private final String endpoint;
    private final String apiKey;
    private final String model;

    public OpenAICompatibleProvider(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) throws Exception {
        String body = "{"
                + "\"model\":\"" + escape(model) + "\","
                + "\"temperature\":" + AIConfig.temperature + ","
                + "\"max_tokens\":" + AIConfig.maxTokens + ","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escape(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escape(userMessage) + "\"}"
                + "]}";

        HttpURLConnection connection =
                (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(AIConfig.connectTimeoutMs);
        connection.setReadTimeout(AIConfig.readTimeoutMs);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Ollama ignores this header. Other OpenAI-compatible providers can use it.
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }

        OutputStream output = connection.getOutputStream();
        output.write(body.getBytes(StandardCharsets.UTF_8));
        output.close();

        int code = connection.getResponseCode();
        InputStream input = (code >= 200 && code < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        String response = readAll(input);
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + response);
        }

        return extractContent(response);
    }

    private static String readAll(InputStream input) throws IOException {
        if (input == null) return "";
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        return result.toString();
    }

    private static String extractContent(String json) {
        String marker = "\"content\"";
        int start = json.indexOf(marker);
        if (start < 0) return json;

        start = json.indexOf(':', start);
        if (start < 0) return json;

        start++;
        while (start < json.length()
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length() || json.charAt(start) != '"') {
            return json;
        }

        StringBuilder result = new StringBuilder();
        boolean escape = false;

        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escape) {
                if (c == 'n') result.append('\n');
                else if (c == 'r') result.append('\r');
                else if (c == 't') result.append('\t');
                else if (c == '"') result.append('"');
                else if (c == '\\') result.append('\\');
                else result.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                return result.toString();
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
