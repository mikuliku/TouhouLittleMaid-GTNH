package com.mikuliku.touhoulittlemaidgtnh.ai;

import com.mikuliku.touhoulittlemaidgtnh.entity.EntityMaidJiuHu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MaidChatManager {
    private static final ExecutorService EXECUTOR =
            Executors.newCachedThreadPool();

    private MaidChatManager() {}

    public static void request(final EntityMaidJiuHu maid,
                               final EntityPlayer player,
                               final String message) {
        if (!maid.canChat()) {
            player.addChatMessage(
                    new ChatComponentText("§d酒狐§f：请稍等一下，主人～"));
            return;
        }

        maid.setChatCooldown(AIConfig.chatCooldownTicks);

        player.addChatMessage(
                new ChatComponentText("§d酒狐§f：让我想一下……"));

        final String playerMessage = message;

        EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                final String reply;

                try {
                    reply = LLMClient.chat(
                            player.getUniqueID(),
                            playerMessage);
                } catch (Exception e) {
                    player.addChatMessage(
                            new ChatComponentText(
                                    "§d酒狐§f：连接 AI 失败："
                                            + e.getMessage()));
                    return;
                }

                player.addChatMessage(
                        new ChatComponentText("§d酒狐§f：" + reply));
            }
        });
    }
}
