package com.mikuliku.touhoulittlemaidgtnh.ai;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class MaidMainThreadScheduler {

    private static final Queue<Runnable> QUEUE =
            new ConcurrentLinkedQueue<Runnable>();

    public static void execute(Runnable task) {
        if (task != null) {
            QUEUE.offer(task);
        }
    }

    public static <T> T callAndWait(
            final Callable<T> callable,
            long timeout,
            TimeUnit unit) throws Exception {

        final CountDownLatch latch =
                new CountDownLatch(1);

        final AtomicReference<T> result =
                new AtomicReference<T>();

        final AtomicReference<Throwable> error =
                new AtomicReference<Throwable>();

        execute(new Runnable() {
            @Override
            public void run() {
                try {
                    result.set(callable.call());
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }
        });

        if (!latch.await(timeout, unit)) {
            throw new RuntimeException(
                    "Minecraft main thread did not answer in time.");
        }

        Throwable throwable = error.get();

        if (throwable != null) {
            if (throwable instanceof Exception) {
                throw (Exception) throwable;
            }

            throw new RuntimeException(throwable);
        }

        return result.get();
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
                // 不让单个 AI/查询任务破坏服务器 tick。
            }
        }
    }
}
