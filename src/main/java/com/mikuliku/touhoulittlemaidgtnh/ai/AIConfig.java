package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class AIConfig {
    public static boolean enabled = false;

    public static String apiUrl =
            "http://localhost:11434/v1/chat/completions";
    public static String apiKey = "";
    public static String model = "qwen3:4b-instruct";

    public static double temperature = 0.7D;
    public static int maxTokens = 512;

    public static int connectTimeoutMs = 5000;
    public static int readTimeoutMs = 120000;

    public static boolean requireChatPrefix = true;
    public static String chatPrefix = "酒狐";
    public static int chatRange = 16;
    public static int chatCooldownTicks = 20;
    public static int memoryTurns = 8;

    public static boolean storageEnabled = true;
    public static int storageRadius = 8;
    public static int storageMaxContainers = 64;
    public static int storageMaxTake = 256;

    private static Configuration config;

    private AIConfig() {}

    public static void load(File configDir) {
        config = new Configuration(
                new File(configDir, "touhoulittlemaidgtnh.cfg"));
        sync();
    }

    public static void sync() {
        enabled = config.getBoolean(
                "enabled", "AI", false,
                "Enable 酒狐 AI chat.");

        apiUrl = config.getString(
                "apiUrl", "AI",
                "http://localhost:11434/v1/chat/completions",
                "OpenAI-compatible endpoint. Default is local Ollama.");

        apiKey = config.getString(
                "apiKey", "AI", "",
                "API key. Leave empty when using Ollama.");

        model = config.getString(
                "model", "AI", "qwen3:4b-instruct",
                "Ollama model or OpenAI-compatible model name.");

        temperature = config.getFloat(
                "temperature", "AI", 0.7F, 0.0F, 2.0F,
                "Sampling temperature.");

        maxTokens = config.getInt(
                "maxTokens", "AI", 512, 32, 4096,
                "Maximum response tokens.");

        connectTimeoutMs = config.getInt(
                "connectTimeoutMs", "AI", 5000,
                1000, 60000,
                "Connection timeout in milliseconds.");

        readTimeoutMs = config.getInt(
                "readTimeoutMs", "AI", 120000,
                5000, 600000,
                "AI response timeout in milliseconds.");

        requireChatPrefix = config.getBoolean(
                "requireChatPrefix", "Chat", true,
                "Only messages beginning with chatPrefix are sent to 酒狐.");

        chatPrefix = config.getString(
                "chatPrefix", "Chat", "酒狐",
                "Prefix used to talk to 酒狐.");

        chatRange = config.getInt(
                "chatRange", "Chat", 16,
                2, 64,
                "Maximum distance between player and owned 酒狐.");

        chatCooldownTicks = config.getInt(
                "chatCooldownTicks", "Chat", 20,
                0, 600,
                "Cooldown between requests in ticks.");

        memoryTurns = config.getInt(
                "memoryTurns", "Chat", 8,
                1, 32,
                "Number of recent user/assistant turns retained.");

        storageEnabled = config.getBoolean(
                "storageEnabled", "Storage", true,
                "Allow 酒狐 AI tools to inspect and take items from nearby IInventory containers.");

        storageRadius = config.getInt(
                "storageRadius", "Storage", 8,
                1, 16,
                "Radius around the player used when searching nearby containers.");

        storageMaxContainers = config.getInt(
                "storageMaxContainers", "Storage", 64,
                1, 256,
                "Maximum number of container tile entities inspected in one storage scan.");

        storageMaxTake = config.getInt(
                "storageMaxTake", "Storage", 256,
                1, 4096,
                "Maximum number of items one AI storage_take call may move to the player.");

        if (config.hasChanged()) {
            config.save();
        }
    }
}
