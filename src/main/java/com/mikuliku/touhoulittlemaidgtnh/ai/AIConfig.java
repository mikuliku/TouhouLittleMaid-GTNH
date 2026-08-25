package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class AIConfig {
    public static boolean enabled = false;

    // Default: local Ollama. No API key and no internet are required.
    public static String apiUrl =
            "http://localhost:11434/v1/chat/completions";
    public static String apiKey = "";
    public static String model = "qwen3:4b-instruct";

    public static double temperature = 0.7D;
    public static int maxTokens = 512;

    public static int connectTimeoutMs = 5000;
    public static int readTimeoutMs = 120000;

    private static Configuration config;

    private AIConfig() {}

    public static void load(File configDir) {
        File file = new File(configDir, "touhoulittlemaidgtnh.cfg");
        config = new Configuration(file);
        sync();
    }

    public static void sync() {
        enabled = config.getBoolean(
                "enabled",
                "AI",
                false,
                "Enable 酒狐 AI chat."
        );

        apiUrl = config.getString(
                "apiUrl",
                "AI",
                "http://localhost:11434/v1/chat/completions",
                "OpenAI-compatible endpoint. Default is local Ollama."
        );

        apiKey = config.getString(
                "apiKey",
                "AI",
                "",
                "API key. Leave empty when using Ollama."
        );

        model = config.getString(
                "model",
                "AI",
                "qwen3:4b-instruct",
                "Local Ollama model or OpenAI-compatible model name."
        );

        temperature = config.getFloat(
                "temperature",
                "AI",
                0.7F,
                0.0F,
                2.0F,
                "Sampling temperature."
        );

        maxTokens = config.getInt(
                "maxTokens",
                "AI",
                512,
                32,
                4096,
                "Maximum response tokens."
        );

        connectTimeoutMs = config.getInt(
                "connectTimeoutMs",
                "AI",
                5000,
                1000,
                60000,
                "Connection timeout in milliseconds."
        );

        readTimeoutMs = config.getInt(
                "readTimeoutMs",
                "AI",
                120000,
                5000,
                600000,
                "AI response timeout in milliseconds."
        );

        if (config.hasChanged()) {
            config.save();
        }
    }
}
