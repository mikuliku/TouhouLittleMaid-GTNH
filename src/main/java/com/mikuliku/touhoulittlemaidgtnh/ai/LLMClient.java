package com.mikuliku.touhoulittlemaidgtnh.ai;

import net.minecraft.entity.player.EntityPlayer;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
        return chatInternal(null, playerId, userMessage);
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

                /*
                 * 每个需要访问Minecraft世界的工具自己负责切换到
                 * 主线程。RecipeSearchTool和StorageContainerTool
                 * 都使用MaidMainThreadScheduler.callAndWait。
                 */
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
                .append("你可以查询游戏配方，也可以检查附近容器并取出材料，")
                .append("但不能虚构游戏数据或声称完成了没有执行的操作。")
                .append("\n\n");

        system.append(
                "当玩家询问“怎么做、怎么合成、需要什么机器、"
                + "某物品的GT配方”等问题时，")
                .append("如果需要游戏数据，请严格输出：")
                .append("\n[[TOOL:recipe_search]]")
                .append("\n{\"query\":\"物品名称\"}")
                .append("\n不要在工具调用前编造配方。")
                .append("\n工具返回结果后，再用中文解释。")
                .append("\n\n");

        system.append(
                "当玩家要求酒狐查看或拿取附近箱子里的材料时，")
                .append("先输出：")
                .append("\n[[TOOL:storage_container]]")
                .append("\n{\"action\":\"scan\"}")
                .append("\n根据扫描结果选择精确物品名称后，再输出：")
                .append("\n[[TOOL:storage_container]]")
                .append("\n{\"action\":\"take\",\"query\":\"精确物品名称\",\"amount\":数量}")
                .append("\n只有工具返回成功后，才能告诉玩家已经取出材料。")
                .append("\n如果工具提示多个相似物品，不要自行猜测。")
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
