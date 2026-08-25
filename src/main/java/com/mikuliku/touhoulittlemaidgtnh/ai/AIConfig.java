package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class AIConfig {
    public static boolean enabled = false;
    public static String apiUrl = "https://api.openai.com/v1/chat/completions";
    public static String apiKey = "";
    public static String model = "gpt-4o-mini";
    public static double temperature = 0.7D;
    public static int maxTokens = 512;

    private static Configuration config;

    private AIConfig() {}

    public static void load(File configDir) {
        File file = new File(configDir, "touhoulittlemaidgtnh.cfg");
        config = new Configuration(file);
        sync();
    }

    public static void sync() {
        enabled = config.getBoolean("enabled", "AI", false,
                "Enable AI chat.");
        apiUrl = config.getString("apiUrl", "AI",
                apiUrl, "OpenAI-compatible chat completions endpoint.");
        apiKey = config.getString("apiKey", "AI",
                "", "API key. Never commit this file to GitHub.");
        model = config.getString("model", "AI",
                model, "Model name.");
        temperature = config.getFloat("temperature", "AI",
                0.7F, 0.0F, 2.0F, "Sampling temperature.");
        maxTokens = config.getInt("maxTokens", "AI",
                512, 32, 8192, "Maximum response tokens.");

        if (config.hasChanged()) config.save();
    }
}
