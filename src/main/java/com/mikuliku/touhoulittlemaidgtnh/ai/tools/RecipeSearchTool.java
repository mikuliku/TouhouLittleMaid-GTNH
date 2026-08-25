package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.MaidMainThreadScheduler;
import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Forge 1.7.10 / GTNH 配方查询工具。
 *
 * 不直接依赖 GregTech 的编译期类，运行时通过反射探测 GT5U，
 * 因此即使玩家没有安装 GregTech，本模组仍然可以正常构建和运行。
 */
public final class RecipeSearchTool implements Tool {

    @Override
    public String getName() {
        return "recipe_search";
    }

    @Override
    public String getDescription() {
        return "查询 Minecraft/GTNH 中如何制作指定物品。"
                + "参数直接填写物品名称，例如：iron plate、钢板、碳纳米管。"
                + "会同时查询 Forge 合成表以及运行中的 GregTech RecipeMap。"
                + "这是只读工具，不会实际消耗物品或操作机器。";
    }

    @Override
    public ToolResult execute(
            final ToolContext context,
            final String arguments) {

        final String query = extractQuery(arguments);

        if (query.length() == 0) {
            return ToolResult.failure("没有提供要查询的物品名称。");
        }

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            return RecipeKnowledge.query(query, 8);
                        }
                    },
                    8,
                    TimeUnit.SECONDS);

            return ToolResult.success(result);

        } catch (Exception e) {
            return ToolResult.failure(
                    "查询配方失败："
                            + e.getClass().getSimpleName()
                            + " "
                            + String.valueOf(e.getMessage()));
        }
    }

    private static String extractQuery(String arguments) {
        if (arguments == null) {
            return "";
        }

        String value = arguments.trim();

        // 兼容模型可能产生的简单 JSON：
        // {"query":"钢板"}
        int q = value.indexOf("\"query\"");
        if (q >= 0) {
            int colon = value.indexOf(':', q);
            if (colon >= 0) {
                int first = value.indexOf('"', colon + 1);
                if (first >= 0) {
                    int second = value.indexOf('"', first + 1);
                    if (second > first) {
                        return value.substring(first + 1, second).trim();
                    }
                }
            }
        }

        return value;
    }

    private static final class RecipeKnowledge {

        private static String query(String query, int maxResults) {
            String lower = query.toLowerCase();
            StringBuilder out = new StringBuilder();
            int found = 0;

            out.append("配方查询：").append(query).append('\n');

            // 1. Forge 1.7.10 原版/普通 Forge IRecipe。
            List<?> recipes = CraftingManager.getInstance().getRecipeList();

            for (Object obj : recipes) {
                if (!(obj instanceof IRecipe)) {
                    continue;
                }

                IRecipe recipe = (IRecipe) obj;
                ItemStack output = recipe.getRecipeOutput();

                if (!matches(output, lower)) {
                    continue;
                }

                out.append(formatCraftingRecipe(recipe, output))
                        .append('\n');

                found++;

                if (found >= maxResults) {
                    return out.toString();
                }
            }

            // 2. GregTech 5U。
            try {
                found = appendGregTechResults(
                        out, lower, maxResults - found, found);
            } catch (Throwable ignored) {
                // GT 不存在或 API 与当前 GTNH 版本不同，忽略。
            }

            if (found == 0) {
                out.append("没有找到匹配的已注册配方。");
            }

            return out.toString();
        }

        private static boolean matches(ItemStack stack, String query) {
            if (stack == null) {
                return false;
            }

            String display = safeLower(stack.getDisplayName());
            String unlocalized = safeLower(stack.getUnlocalizedName());

            return display.contains(query)
                    || unlocalized.contains(query);
        }

        private static String formatCraftingRecipe(
                IRecipe recipe,
                ItemStack output) {

            StringBuilder s = new StringBuilder();

            s.append("[Forge合成] 输出=")
                    .append(stackName(output))
                    .append(" x")
                    .append(output.stackSize);

            try {
                ItemStack[] inputs = recipe.getInput();

                s.append("；输入=");
                boolean first = true;

                if (inputs != null) {
                    for (ItemStack input : inputs) {
                        if (input == null) {
                            continue;
                        }

                        if (!first) {
                            s.append(" + ");
                        }

                        s.append(stackName(input))
                                .append(" x")
                                .append(input.stackSize);

                        first = false;
                    }
                }
            } catch (Throwable ignored) {
                // 某些特殊 IRecipe 不提供普通输入数组。
            }

            return s.toString();
        }

        @SuppressWarnings("unchecked")
        private static int appendGregTechResults(
                StringBuilder out,
                String query,
                int remaining,
                int found) throws Exception {

            if (remaining <= 0) {
                return found;
            }

            Class<?> recipeMapClass =
                    Class.forName("gregtech.api.recipe.RecipeMap");

            Field allMaps =
                    recipeMapClass.getField("ALL_RECIPE_MAPS");

            Object mapObject = allMaps.get(null);

            if (!(mapObject instanceof Map)) {
                return found;
            }

            Map<Object, Object> maps =
                    (Map<Object, Object>) mapObject;

            Method getBackend =
                    recipeMapClass.getMethod("getBackend");

            for (Map.Entry<Object, Object> entry : maps.entrySet()) {
                if (found >= remaining + found) {
                    break;
                }

                Object map = entry.ge
