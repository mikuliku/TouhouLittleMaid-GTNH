package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.MaidMainThreadScheduler;
import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * GT5U 安全执行桥。
 *
 * 执行前检查：
 * 1. 必须在服务器线程；
 * 2. 控制器必须存在且已经成型（若该字段可访问）；
 * 3. 机器不能正在运行；
 * 4. 可选地确认当前机器 RecipeMap 中存在指定输出。
 *
 * 通过 GT5U 自身的 checkProcessing() 让机器按照真实 RecipeMap
 * 选择并消费输入，而不是在本模组中复制 GTNH 配方逻辑。
 */
public final class GT5UMachineExecutorTool implements Tool {

    @Override
    public String getName() {
        return "gt5u_machine_execute";
    }

    @Override
    public String getDescription() {
        return "启动指定GT5U多方块的真实配方检查/加工。参数：{\"x\":X,\"y\":Y,\"z\":Z,\"outputQuery\":\"可选输出名称\"}。执行前必须先把材料放入输入总线。";
    }

    @Override
    public ToolResult execute(
            final ToolContext context,
            final String argumentsJson) {

        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        final EntityPlayer player = context.getPlayer();
        final int x = getInt(argumentsJson, "x", Integer.MIN_VALUE);
        final int y = getInt(argumentsJson, "y", Integer.MIN_VALUE);
        final int z = getInt(argumentsJson, "z", Integer.MIN_VALUE);
        final String outputQuery = getString(argumentsJson, "outputQuery");

        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            return ToolResult.failure("缺少控制器坐标x/y/z。请先使用gt5u_machine_scan。");
        }

        if (player.worldObj.isRemote) {
            return ToolResult.failure("GT5U执行只能在服务器线程进行。");
        }

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            return executeMachine(
                                    player, x, y, z, outputQuery);
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
                    "GT5U执行失败："
                            + e.getClass().getSimpleName()
                            + " "
                            + String.valueOf(e.getMessage()));
        }
    }

    private static String executeMachine(
            EntityPlayer player,
            int x,
            int y,
            int z,
            String outputQuery) {

        TileEntity controller = player.worldObj.getTileEntity(x, y, z);
        if (controller == null) {
            return "指定坐标没有TileEntity。";
        }

        Object meta = invokeNoArg(controller, "getMetaTileEntity");
        if (meta == null) {
            return "指定坐标不是GregTech机器控制器。";
        }

        Boolean machineFormed = getBooleanField(meta, "mMachine");
        if (Boolean.FALSE.equals(machineFormed)) {
            return "GT5U多方块当前未成型，已阻止执行。";
        }

        int progress = getIntField(meta, "mProgresstime", 0);
        int maxProgress = getIntField(meta, "mMaxProgresstime", 0);
        if (maxProgress > 0 && progress < maxProgress) {
            return "机器当前正在运行（" + progress + "/" + maxProgress + " ticks），已阻止重复启动。";
        }

        Object recipeMap = invokeNoArg(meta, "getRecipeMap");
        if (recipeMap == null) {
            return "该控制器没有可用的RecipeMap，无法使用通用GT5U执行桥。";
        }

        if (outputQuery.length() > 0
                && !recipeMapContainsOutput(recipeMap, outputQuery)) {
            return "当前机器的RecipeMap 中没有找到输出“"
                    + outputQuery + "”对应的真实配方，已阻止执行。";
        }

        Object started = invokeNoArg(meta, "startRecipeProcessing");
        if (started == INVOKE_FAILED) {
            return "无法进入GT5U配方处理阶段，已阻止执行。";
        }

        Object checkResult = null;
        try {
            checkResult = invokeNoArg(meta, "checkProcessing");
        } finally {
            invokeNoArg(meta, "endRecipeProcessing");
        }

        if (checkResult == null || checkResult == INVOKE_FAILED) {
            return "GT5U checkProcessing() 未返回有效结果，已停止执行。";
        }

        Boolean successful = invokeBooleanNoArg(checkResult, "wasSuccessful");
        String resultId = String.valueOf(
                invokeNoArg(checkResult, "getID"));

        if (!Boolean.TRUE.equals(successful)) {
            return "GT5U配方检查失败，机器未启动。结果=" + resultId
                    + "；请检查材料、流体、能量、维护和机器结构。";
        }

        invokeNoArg(meta, "updateSlots");

        return "OK:GT5U配方检查成功，机器已进入加工状态。结果=" + resultId;
    }

    private static boolean recipeMapContainsOutput(
            Object recipeMap,
            String query) {

        Object recipes = invokeNoArg(recipeMap, "getAllRecipes");
        if (!(recipes instanceof Iterable)) {
            return false;
        }

        String q = query.toLowerCase();

        for (Object recipe : (Iterable<?>) recipes) {
            if (recipe == null) {
                continue;
            }

            Object outputs = getFieldValue(recipe, "mOutputs");
            if (!(outputs instanceof Object[])) {
                continue;
            }

            for (Object value : (Object[]) outputs) {
                if (!(value instanceof ItemStack)) {
                    continue;
                }

                ItemStack stack = (ItemStack) value;
                String display = stack.getDisplayName();
                String unlocalized = stack.getUnlocalizedName();

                if ((display != null && display.toLowerCase().contains(q))
                        || (unlocalized != null
                        && unlocalized.toLowerCase().contains(q))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static final Object INVOKE_FAILED = new Object();

    private static Object invokeNoArg(Object target, String name) {
        if (target == null) {
            return INVOKE_FAILED;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return INVOKE_FAILED;
            }
        }

        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return INVOKE_FAILED;
        }
    }

    private static Object invokeOneArg(
            Object target,
            String name,
            Object arg) {

        if (target == null) {
            return INVOKE_FAILED;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name)
                        || method.getParameterTypes().length != 1) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    return method.invoke(target, arg);
                } catch (Throwable ignored) {
                    return INVOKE_FAILED;
                }
            }
            type = type.getSuperclass();
        }

        return INVOKE_FAILED;
    }

    private static Object getFieldValue(Object target, String name) {
        if (target == null) {
            return null;
        }

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

    private static int getIntField(
            Object target,
            String name,
            int fallback) {

        Object value = getFieldValue(target, name);
        return value instanceof Number
                ? ((Number) value).intValue()
                : fallback;
    }

    private static Boolean getBooleanField(Object target, String name) {
        Object value = getFieldValue(target, name);
        return value instanceof Boolean
                ? (Boolean) value
                : null;
    }

    private static Boolean invokeBooleanNoArg(
            Object target,
            String name) {
        Object value = invokeNoArg(target, name);
        return value instanceof Boolean
                ? (Boolean) value
                : null;
    }

    private static String getString(String json, String key) {
        if (json == null) {
            return "";
        }

        String token = "\"" + key + "\"";
        int p = json.indexOf(token);
        if (p < 0) {
            return "";
        }

        int colon = json.indexOf(':', p + token.length());
        if (colon < 0) {
            return "";
        }

        int first = json.indexOf('"', colon + 1);
        if (first < 0) {
            return "";
        }

        int second = json.indexOf('"', first + 1);
        if (second <= first) {
            return "";
        }

        return json.substring(first + 1, second).trim();
    }

    private static int getInt(
            String json,
            String key,
            int fallback) {

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

        boolean negative = start < json.length()
                && json.charAt(start) == '-';
        if (negative) {
            start++;
        }

        int end = start;
        while (end < json.length()
                && Character.isDigit(json.charAt(end))) {
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
