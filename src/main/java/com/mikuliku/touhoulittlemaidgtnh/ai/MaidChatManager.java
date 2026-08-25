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

    public static void request(
            final EntityMaidJiuHu maid,
            final EntityPlayer player,
            final String message) {

        if (!maid.canChat()) {
            player.addChatMessage(
                    new ChatComponentText(
                            "§d酒狐§f：请稍等一下，主人～"));
            return;
        }

        maid.setChatCooldown(AIConfig.chatCooldownTicks);

        player.addChatMessage(
                new ChatComponentText(
                        "§d酒狐§f：让我想一下……"));

        EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final String reply = LLMClient.chat(
                            player.getUniqueID(), message);

                    MaidMainThreadScheduler.execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (!player.isDead) {
                                        player.addChatMessage(
                                                new ChatComponentText(
                                                        "§d酒狐§f：" + reply));
                                    }
                                }
                            });

                } catch (final Exception e) {
                    MaidMainThreadScheduler.execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (!player.isDead) {
                                        player.addChatMessage(
                                                new ChatComponentText(
                                                        "§d酒狐§f：连接 AI 失败："
                                                                + safeMessage(e)));
                                    }
                                }
                            });
                }
            }
        });
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();

        if (message == null || message.length() == 0) {
            return e.getClass().getSimpleName();
        }

        if (message.length() > 180) {
            return message.substring(0, 180) + "...";
        }

        return message;
    }
}
