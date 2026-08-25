package com.mikuliku.touhoulittlemaidgtnh.ai;

import com.mikuliku.touhoulittlemaidgtnh.entity.EntityMaidJiuHu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MaidChatManager {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private MaidChatManager() {}

    public static void request(final EntityMaidJiuHu maid,
                               final EntityPlayer player,
                               final String message) {
        if (!maid.canChat()) {
            player.addChatMessage(new ChatComponentText("§d酒狐§f：请稍等一下，主人～"));
            return;
        }

        maid.setChatCooldown(20);
        player.addChatMessage(new ChatComponentText("§d酒狐§f：让我想一下……"));

        EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                final String reply;
                try {
                    reply = LLMClient.chat(message);
                } catch (Exception e) {
                    player.addChatMessage(new ChatComponentText(
                            "§d酒狐§f：连接 AI 时出了问题：" + e.getClass().getSimpleName()));
                    return;
                }

                // 第一阶段：直接发送结果。
                // 后续版本会通过服务器主线程任务队列安全回调，并加入 Tool 系统。
                player.addChatMessage(new ChatComponentText("§d酒狐§f：" + reply));
            }
        });
    }
}
