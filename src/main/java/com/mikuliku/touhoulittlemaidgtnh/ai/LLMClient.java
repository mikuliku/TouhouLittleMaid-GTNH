package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.io.IOException;

public final class LLMClient {
    private LLMClient() {}

    public static String chat(String userMessage) throws IOException {
        if (!AIConfig.enabled) {
            return "AI 功能尚未开启。请在配置文件中设置 enabled=true。";
        }

        String system =
                "你是 Minecraft GTNH 整合包中的女仆“酒狐”。"
                + "你必须使用中文回答玩家。"
                + "语气亲切、简洁、可靠，像一名真正的游戏女仆。"
                + "当前版本只能聊天，不要声称已经执行了任何尚未执行的游戏操作。";

        try {
            return AIProviderFactory.create().chat(system, userMessage);
        } catch (Exception e) {
            throw new IOException("AI provider request failed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
