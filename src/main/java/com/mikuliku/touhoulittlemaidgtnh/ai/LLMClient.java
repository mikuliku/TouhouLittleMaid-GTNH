package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class LLMClient {
    private LLMClient() {}

    public static String chat(String userMessage) throws IOException {
        if (!AIConfig.enabled) return "AI 功能尚未开启。请在配置文件中设置 enabled=true。";
        if (AIConfig.apiKey == null || AIConfig.apiKey.trim().isEmpty()) {
            return "主人还没有配置 API Key。";
        }

        String system =
                "你是 Minecraft GTNH 整合包中的女仆“酒狐”。" +
                "你必须使用中文回答玩家。语气亲切、简洁、像一名可靠的女仆。" +
                "不要声称自己已经执行了游戏操作，除非游戏程序明确告诉你操作成功。";

        String body = "{"
                + "\"model\":\"" + jsonEscape(AIConfig.model) + "\","
                + "\"temperature\":" + AIConfig.temperature + ","
                + "\"max_tokens\":" + AIConfig.maxTokens + ","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + jsonEscape(system) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(userMessage) + "\"}"
                + "]}";

        URL url = new URL(AIConfig.apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + AIConfig.apiKey);

        OutputStream out = connection.getOutputStream();
        out.write(body.getBytes(StandardCharsets.UTF_8));
        out.close();

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        String response = readAll(stream);
        if (code < 200 || code >= 300) {
            return "AI 请求失败（HTTP " + code + "）： " + response;
        }

        return extractContent(response);
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line);
        reader.close();
        return result.toString();
    }

    // 第一阶段只解析 OpenAI-compatible 常见 JSON：
    // {"choices":[{"message":{"content":"..."}}]}
    private static String extractContent(String json) {
        String marker = "\"content\"";
        int start = json.indexOf(marker);
        if (start < 0) return json;
        start = json.indexOf(':', start);
        if (start < 0) return json;
        start++;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '"') return json;

        StringBuilder result = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                if (c == 'n') result.append('\n');
                else if (c == 'r') result.append('\r');
                else if (c == 't') result.append('\t');
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

    private static String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
