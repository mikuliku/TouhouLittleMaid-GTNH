package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.MaidMainThreadScheduler;
import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * GTNH 自动生产最终状态查询工具。
 *
 * 只读取机器状态，不启动、停止或修改机器。
 * 用于让 AI 在启动自动生产后确认：是否运行、进度、输出槽中已有何物，
 * 以及多方块是否仍然成型。
 */
public final class GT5UProductionStatusTool implements Tool {

    @Override
    public String getName() {
        return "gt5u_production_status";
    }

    @Override
    public String getDescription() {
        return "查询GT5U生产状态。参数：{\"x\":X,\"y\":Y,\"z\":Z}。只读，不启动或修改机器。";
    }

    @Override
    public ToolResult execute(final ToolContext context, final String argumentsJson) {
        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        final EntityPlayer player = context.getPlayer();
        final int x = getInt(argumentsJson, "x", Integer.MIN_VALUE);
        final int y = getInt(argumentsJson, "y", Integer.MIN_VALUE);
        final int z = getInt(argumentsJson, "z", Integer.MIN_VALUE);

        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            return ToolResult.failure("缺少控制器坐标x/y/z。");
        }
        if (player.worldObj.isRemote) {
            return ToolResult.failure("生产状态查询只能在服务器侧进行。");
        }

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            return readStatus(player, x, y, z);
                        }
                    },
                    8,
                    TimeUnit.SECONDS);

            if (result.startsWith("OK:")) {
                return ToolResult.success(result.substring(3));
            }
            return ToolResult.failure(result);
        } catch (Exception e) {
            return ToolResult.failure(
                    "GT5U状态查询失败："
                            + e.getClass().getSimpleName()
                            + " "
                            + String.valueOf(e.getMessage()));
        }
    }

    private static String readStatus(EntityPlayer player, int x, int y, int z) {
        TileEntity controller = player.worldObj.getTileEntity(x, y, z);
        if (controller == null) {
            return "指定坐标没有TileEntity。";
        }

        Object meta = getMeta(controller);
        if (meta == null) {
            return "指定坐标不是GregTech机器控制器。";
        }

        Boolean formed = getBooleanField(meta, "mMachine");
        int progress = getIntField(meta, "mProgresstime", 0);
        int maxProgress = getIntField(meta, "mMaxProgresstime", 0);

        StringBuilder out = new StringBuilder();
        out.append("GT5U生产状态：\n");

        if (formed != null) {
            out.append("结构：").append(formed ? "已成型" : "未成型").append('\n');
            if (!formed) {
                return "OK:" + out + "机器当前未成型。";
            }
        } else {
            out.append("结构：无法读取成型字段\n");
        }

        boolean running = maxProgress > 0 && progress < maxProgress;
        out.append("运行：").append(running ? "运行中" : "未运行").append('\n');

        if (maxProgress > 0) {
            out.append("进度：").append(progress).append('/')
                    .append(maxProgress).append(" ticks\n");
        } else {
            out.append("进度：当前无可用进度数据\n");
        }

        List<ItemStack> outputs = collectStacks(meta, "mOutputBusses", "mOutputHatches");
        if (outputs.isEmpty()) {
            out.append("输出：当前未检测到可读取的输出物品。\n");
        } else {
            out.append("输出：\n");
            for (ItemStack stack : outputs) {
                if (stack != null && stack.stackSize > 0) {
                    out.append("- ")
                            .append(stack.getDisplayName())
                            .append(" x")
                            .append(stack.stackSize)
                            .append('\n');
                }
            }
        }

        if (running) {
            out.append("结论：机器正在加工。");
        } else {
            out.append("结论：机器当前没有运行中的加工任务。");
        }

        return "OK:" + out.toString();
    }

    private static Object getMeta(TileEntity controller) {
        Class<?> type = controller.getClass();
        while (type != null) {
            try {
                java.lang.reflect.Method method = type.getDeclaredMethod("getMetaTileEntity");
                method.setAccessible(true);
                return method.invoke(controller);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static List<ItemStack> collectStacks(Object meta, String... fieldNames) {
        List<ItemStack> result = new ArrayList<ItemStack>();

        for (String fieldName : fieldNames) {
            Object value = getFieldValue(meta, fieldName);
            if (!(value instanceof Iterable)) {
                continue;
            }

            for (Object hatch : (Iterable<?>) value) {
                if (hatch == null) {
                    continue;
                }

                Integer size = invokeIntNoArg(hatch, "getSizeInventory");
                if (size == null || size <= 0) {
                    continue;
                }

                for (int slot = 0; slot < size; slot++) {
                    Object stack = invokeOneArg(hatch, "getStackInSlot", Integer.valueOf(slot));
                    if (stack instanceof ItemStack) {
                        ItemStack item = (ItemStack) stack;
                        if (item.stackSize > 0) {
                            result.add(item.copy());
                        }
                    }
                }
            }
        }

        return result;
    }

    private static Object getFieldValue(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static Boolean getBooleanField(Object target, String name) {
        Object value = getFieldValue(target, name);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private static int getIntField(Object target, String name, int fallback) {
        Object value = getFieldValue(target, name);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static Integer invokeIntNoArg(Object target, String name) {
        Object value = invokeNoArg(target, name);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private static Object invokeNoArg(Object target, String name) {
        if (target == null) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            try {
                java.lang.reflect.Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }

    private static Object invokeOneArg(Object target, String name, Object arg) {
        if (target == null) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name)
                        || method.getParameterTypes().length != 1) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    return method.invoke(target, arg);
                } catch (Throwable ignored) {
                    return null;
                }
            }
            type = type.getSuperclass();
        }

        return null;
    }

    private static int getInt(String json, String key, int fallback) {
        if (json == null) {
            return fallback;
        }

        String token = "\"" + key + "\"";
        int p = json.indexOf(token);
        if (p < 0) {
            return fallback;
        }

        int colon = json.indexOf(':', p + token.length());
        if (colon < 0) {
            return fallback;
        }

        int start = colon + 1;
        while (start < json.length()
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        boolean negative = start < json.length() && json.charAt(start) == '-';
        if (negative) {
            start++;
        }

        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }

        if (end <= start) {
            return fallback;
        }

        try {
            int value = Integer.parseInt(json.substring(start, end));
            return negative ? -value : value;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
