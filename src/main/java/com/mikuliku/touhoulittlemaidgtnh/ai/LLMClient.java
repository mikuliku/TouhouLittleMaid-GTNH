package com.mikuliku.touhoulittlemaidgtnh.ai;

import net.minecraft.entity.player.EntityPlayer;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * AI 对话入口。
 *
 * 第四步加入了一个兼容老模型的“文本工具调用”协议：
 *
 * [[TOOL:recipe_search]]
 * 要查询的物品
 *
 * 模型不需要原生支持 OpenAI tool_calls。
 */
public final class LLMClient {

    private LLMClient() {}

    public static String chat(String userMessage)
            throws IOException {

        return chat(UUID.randomUUID(), userMessage);
    }

    public static String chat(
            UUID playerId,
            String userMessage)
            throws IOException {

        return chatInternal(
                null,
                playerId,
                userMessage);
    }

    public static String chat(
            EntityPlayer player,
            String userMessage)
            throws IOException {

        if (player == null) {
            return chat(userMessage);
        }

        return chatInternal(
                player,
                player.getUniqueID(),
                userMessage);
    }

    private static String chatInternal(
            EntityPlayer player,
            UUID playerId,
            String userMessage)
            throws IOException {

        if (!AIConfig.enabled) {
            return "AI 功能尚未开启。";
        }

        String system = buildSystemPrompt(playerId);

        String answer;

        try {
            answer = AIProviderFactory.create()
                    .chat(system, userMessage);
        } catch (Exception e) {
            throw new IOException(
                    "AI provider request failed: "
                            + e.getClass().getSimpleName()
                            + ": " + e.getMessage(), e);
        }

        ToolCallParser.Call call =
                ToolCallParser.parse(answer);

        if (call != null
                && player != null) {

            Tool tool =
                    ToolRegistry.get(call.name);

            if (tool != null) {

                ToolResult result =
                        tool.execute(
                                new ToolContext(player),
                                call.arguments);

                String followUp =
                        userMessage
                                + "\n\n[系统：你刚才请求了游戏工具 `"
                                + call.name
                                + "`。]\n"
                                + "[工具结果]\n"
                                + result.content
                                + "\n[/工具结果]\n\n"
                                + "请根据工具结果直接回答玩家。"
                                + "不要再次调用工具，"
                                + "不要声称执行了工具未执行的操作。";

                try {
                    answer = AIProviderFactory.create()
                            .chat(system, followUp);
                } catch (Exception e) {
                    throw new IOException(
                            "AI follow-up request failed: "
                                    + e.getClass().getSimpleName()
                                    + ": " + e.getMessage(), e);
                }
            }
        }

        ConversationMemory.add(
                playerId,
                "user",
                userMessage);

        ConversationMemory.add(
                playerId,
                "assistant",
                answer);

        return answer;
    }

    private static String buildSystemPrompt(
            UUID playerId) {

        List<ChatMessage> history =
                ConversationMemory.get(playerId);

        StringBuilder system =
                new StringBuilder();

        system.append(
                "你是 Minecraft GTNH 整合包中的女仆“酒狐”。")
                .append("必须使用中文回答。")
                .append("语气亲切、简洁、可靠。")
                .append("你可以查询游戏配方，但不能虚构结果。")
                .append("当前可用工具可能包括：")
                .append("\n- recipe_search：查询 Minecraft 和 GregTech "
                        + "配方；参数是物品名称。")
                .append("\n\n");

        system.append(
                "当玩家询问“怎么做、怎么合成、需要什么机器、"
                + "某物品的GT配方”等问题时，")
                .append("如果需要游戏数据，请严格输出：")
                .append("\n[[TOOL:recipe_search]]")
                .append("\n查询物品名称")
                .append("\n不要在工具调用前编造配方。")
                .append("\n工具返回结果后，再用中文解释。")
                .append("\n\n");

        if (ToolRegistry.all().size() > 0) {
            system.append("当前工具：\n")
                    .append(ToolRegistry.describeTools())
                    .append('\n');
        }

        if (history.size() > 0) {
            system.append("最近对话：\n");

            for (ChatMessage message : history) {
                system.append(message.role)
                        .append(": ")
                        .append(message.content)
                        .append('\n');
            }
        }

        return system.toString();
    }

    private static final class ToolCallParser {

        private ToolCallParser() {}

        private static Call parse(String text) {

            if (text == null) {
                return null;
            }

            String marker =
                    "[[TOOL:";

            int start =
                    text.indexOf(marker);

            if (start < 0) {
                return null;
            }

            int end =
                    text.indexOf("]]", start);

            if (end < 0) {
                return null;
            }

            String name =
                    text.substring(
                            start + marker.length(),
                            end).trim();

            if (name.length() == 0) {
                return null;
            }

            String arguments =
                    text.substring(end + 2).trim();

            return new Call(name, arguments);
        }

        private static final class Call {

            private final String name;
            private final String arguments;

            private Call(
                    String name,
                    String arguments) {
                this.name = name;
                this.arguments = arguments;
            }
        }
    }
}
