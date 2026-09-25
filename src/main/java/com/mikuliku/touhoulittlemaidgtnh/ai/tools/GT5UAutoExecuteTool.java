package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * GTNH 自动生产编排器（Stage 11）。
 *
 * 流程：
 * 1. 根据目标输出查询当前机器自己的 RecipeMap；
 * 2. 找到真实 GTRecipe；
 * 3. 按真实 mInputs 逐项调用 GT5U 输入总线转移工具；
 * 4. 所有物品输入齐全后调用 GT5U 执行工具。
 *
 * 本工具不复制 GTNH 配方规则。最终配方检查仍由 GT5U checkProcessing() 完成。
 * 流体输入暂由执行器负责检查；没有自动流体搬运时会返回明确的缺少流体信息。
 */
public final class GT5UAutoExecuteTool implements Tool {

    @Override
    public String getName() {
        return "gt5u_auto_execute";
    }

    @Override
    public String getDescription() {
        return "自动执行指定GT5U机器的目标产物：查真实RecipeMap、按配方取物品、装入输入总线并启动机器。参数：{\"x\":X,\"y\":Y,\"z\":Z,\"outputQuery\":\"目标产物\"}。";
    }

    @Override
    public ToolResult execute(ToolContext context, String argumentsJson) {
        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        EntityPlayer player = context.getPlayer();
        int x = getInt(argumentsJson, "x", Integer.MIN_VALUE);
        int y = getInt(argumentsJson, "y", Integer.MIN_VALUE);
        int z = getInt(argumentsJson, "z", Integer.MIN_VALUE);
        String outputQuery = getString(argumentsJson, "outputQuery");

        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            return ToolResult.failure("缺少控制器坐标x/y/z。");
        }
        if (outputQuery.length() == 0) {
            return ToolResult.failure("没有指定目标产物outputQuery。");
        }
        if (player.worldObj.isRemote) {
            return ToolResult.failure("自动执行只能在服务器侧进行。");
        }

        TileEntity controller = player.worldObj.getTileEntity(x, y, z);
        if (controller == null) {
            return ToolResult.failure("指定坐标没有GT5U控制器。");
        }

        Object meta = invokeNoArg(controller, "getMetaTileEntity");
        if (meta == null) {
            return ToolResult.failure("指定坐标不是GregTech机器控制器。");
        }

        Object recipeMapObject = invokeNoArg(meta, "getRecipeMap");
        if (!(recipeMapObject instanceof RecipeMap)) {
            return ToolResult.failure("控制器没有可用RecipeMap。");
        }

        RecipeMap<?> recipeMap = (RecipeMap<?>) recipeMapObject;
        GTRecipe recipe = findOutputRecipe(recipeMap, outputQuery);
        if (recipe == null) {
            return ToolResult.failure("当前机器的RecipeMap中没有找到目标输出“" + outputQuery + "”的真实配方。");
        }

        StringBuilder report = new StringBuilder();
        report.append("已选择真实GTNH配方：")
                .append(GT5URecipeAdapter.describe(recipe))
                .append("\n");

        if (recipe.mInputs != null) {
            GT5UMachineTransferTool transfer = new GT5UMachineTransferTool();
            for (ItemStack input : recipe.mInputs) {
                if (input == null || input.stackSize <= 0) {
                    continue;
                }

                String query = input.getDisplayName();
                String args = "{\"x\":" + x
                        + ",\"y\":" + y
                        + ",\"z\":" + z
                        + ",\"query\":\"" + escape(query)
                        + "\",\"amount\":" + input.stackSize + "}";

                ToolResult moved = transfer.execute(context, args);
                if (!moved.success || !containsRequestedAmount(moved.content, input.stackSize)) {
                    return ToolResult.failure(
                            report.toString()
                                    + "材料“" + query + " x" + input.stackSize
                                    + "未能完整装入输入总线。\n"
                                    + moved.content);
                }

                report.append("已装入：")
                        .append(query)
                        .append(" x")
                        .append(input.stackSize)
                        .append("\n");
            }
        }

        GT5UMachineExecutorTool executor = new GT5UMachineExecutorTool();
        String executeArgs = "{\"x\":" + x
                + ",\"y\":" + y
                + ",\"z\":" + z
                + ",\"outputQuery\":\"" + escape(outputQuery) + "\"}";
        ToolResult executed = executor.execute(context, executeArgs);

        if (!executed.success) {
            return ToolResult.failure(report.toString() + "机器启动失败：\n" + executed.content);
        }

        return ToolResult.success(report.toString() + "\n" + executed.content);
    }

    private static GTRecipe findOutputRecipe(RecipeMap<?> map, String query) {
        Collection<GTRecipe> recipes;
        try {
            recipes = map.getAllRecipes();
        } catch (Throwable ignored) {
            return null;
        }
        if (recipes == null) {
            return null;
        }

        String q = query.toLowerCase();
        for (GTRecipe recipe : recipes) {
            if (recipe == null || recipe.mOutputs == null) {
                continue;
            }
            for (ItemStack output : recipe.mOutputs) {
                if (output == null) {
                    continue;
                }
                String display = output.getDisplayName();
                String unlocalized = output.getUnlocalizedName();
                if ((display != null && display.toLowerCase().contains(q))
                        || (unlocalized != null && unlocalized.toLowerCase().contains(q))) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private static boolean containsRequestedAmount(String content, int amount) {
        if (content == null) {
            return false;
        }
        String marker = "已转移 " + amount + " 个";
        return content.contains(marker) && !content.contains("但请求 " + amount + " 个");
    }

    private static Object invokeNoArg(Object target, String name) {
        if (target == null) {
            return null;
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
                return null;
            }
        }
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getString(String json, String key) {
        if (json == null) return "";
        String token = "\"" + key + "\"";
        int p = json.indexOf(token);
        if (p < 0) return "";
        int colon = json.indexOf(':', p + token.length());
        if (colon < 0) return "";
        int first = json.indexOf('"', colon + 1);
        if (first < 0) return "";
        int second = json.indexOf('"', first + 1);
        if (second <= first) return "";
        return json.substring(first + 1, second).trim();
    }

    private static int getInt(String json, String key, int fallback) {
        if (json == null) return fallback;
        String token = "\"" + key + "\"";
        int p = json.indexOf(token);
        if (p < 0) return fallback;
        int colon = json.indexOf(':', p + token.length());
        if (colon < 0) return fallback;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        boolean negative = start < json.length() && json.charAt(start) == '-';
        if (negative) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end <= start) return fallback;
        try {
            int value = Integer.parseInt(json.substring(start, end));
            return negative ? -value : value;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
