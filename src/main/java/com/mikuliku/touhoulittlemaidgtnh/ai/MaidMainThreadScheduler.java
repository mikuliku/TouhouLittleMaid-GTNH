package com.mikuliku.touhoulittlemaidgtnh.ai;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MaidMainThreadScheduler {

    private static final Queue<Runnable> QUEUE =
            new ConcurrentLinkedQueue<Runnable>();

    public static void execute(Runnable task) {
        if (task != null) {
            QUEUE.offer(task);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Runnable task;
        while ((task = QUEUE.poll()) != null) {
            try {
                task.run();
            } catch (Throwable ignored) {
                // 防止单个 AI 回调破坏服务器 tick。
            }
        }
    }
}
