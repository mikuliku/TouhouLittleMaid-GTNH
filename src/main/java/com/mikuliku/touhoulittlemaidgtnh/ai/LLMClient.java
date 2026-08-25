package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class LLMClient {
    private LLMClient() {}

    public static String chat(String userMessage) throws IOException {
        return chat(UUID.randomUUID(), userMessage);
    }

    public static String chat(UUID playerId, String userMessage) throws IOException {
        if (!AIConfig.enabled) {
            return "AI 功能尚未开启。请在配置文件中设置 enabled=true。";
        }

        List<ChatMessage> history = ConversationMemory.get(playerId);

        StringBuilder system = new StringBuilder();

        system.append("你是 Minecraft GTNH 整合包中的女仆“酒狐”。")
                .append("你必须使用中文回答玩家。")
                .append("语气亲切、简洁、可靠，像一名真正的游戏女仆。")
                .append("当前阶段只允许聊天和只读信息查询。")
                .append("绝对不要声称已经执行了尚未执行的游戏操作。")
                .append("\n\n");

        if (ToolRegistry.all().size() > 0) {
            system.append("当前可用的游戏工具说明：\n")
                    .append(ToolRegistry.describeTools())
                    .append("\n工具目前由后续阶段接入 AI Tool Calling。")
                    .append("\n");
        }

        if (history.size() > 0) {
            system.append("\n最近对话：\n");

            for (ChatMessage message : history) {
                system.append(message.role)
                        .append(": ")
                        .append(message.content)
                        .append('\n');
            }
        }

        ConversationMemory.add(playerId, "user", userMessage);

        try {
            String answer = AIProviderFactory.create()
                    .chat(system.toString(), userMessage);

            ConversationMemory.add(playerId, "assistant", answer);
            return answer;
        } catch (Exception e) {
            throw new IOException(
                    "AI provider request failed: "
                            + e.getClass().getSimpleName()
                            + ": " + e.getMessage(), e);
        }
    }
}
