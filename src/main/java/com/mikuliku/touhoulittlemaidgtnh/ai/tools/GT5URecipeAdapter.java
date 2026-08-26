package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GT5U 配方只读适配器。
 *
 * 当前阶段：
 *
 * 1. 读取 GT5U 已注册的全部 RecipeMap
 * 2. 搜索真实 GTRecipe
 * 3. 返回真实配方
 *
 * 注意：
 *
 * 本类目前不会：
 * - 操作机器
 * - 扣除玩家物品
 * - 生成物品
 * - 修改玩家背包
 *
 * 这些功能将在后续的 RecipeSafeExecutor / MachineExecutor
 * 阶段实现。
 */
public final class GT5URecipeAdapter {

    private GT5URecipeAdapter() {
    }

    /**
     * 搜索 GT5U 中与关键词匹配的真实配方。
     *
     * @param query 物品名称或 unlocalized name
     * @return 最多返回 8 个匹配配方
     */
    public static List<GTRecipe> find(String query) {

        List<GTRecipe> result = new ArrayList<GTRecipe>();

        if (query == null) {
            return result;
        }

        String trimmed = query.trim();

        if (trimmed.length() == 0) {
            return result;
        }

        String search = trimmed.toLowerCase();

        /*
         * 不再硬编码：
         *
         * RecipeMaps.assemblerRecipes
         * RecipeMaps.mixerRecipes
         * RecipeMaps...
         *
         * 而是直接遍历 GT5U 注册的全部 RecipeMap。
         *
         * 这样可以：
         *
         * 1. 兼容更多 GT5U 机器
         * 2. 避免不同版本 RecipeMaps 字段名称变化
         * 3. 让 AI 后续可以搜索 GT5U 中更多类型的机器
         */
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {

            if (map == null) {
                continue;
            }

            Collection<GTRecipe> recipes;

            try {
                recipes = map.getAllRecipes();
            } catch (Throwable ignored) {
                /*
                 * 某些特殊 RecipeMap 如果读取失败，
                 * 不应该导致整个 AI 系统崩溃。
                 */
                continue;
            }

            if (recipes == null) {
                continue;
            }

            for (GTRecipe recipe : recipes) {

                if (recipe == null) {
                    continue;
                }

                if (matches(recipe, search)) {

                    result.add(recipe);

                    /*
                     * AI 查询不需要一次返回几千个结果。
                     *
                     * 限制为 8 个，可以避免聊天查询造成
                     * 大量遍历和内存占用。
                     */
                    if (result.size() >= 8) {
                        return result;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 判断一个真实 GTRecipe 是否包含指定物品。
     */
    private static boolean matches(
            GTRecipe recipe,
            String query) {

        if (recipe == null) {
            return false;
        }

        try {

            /*
             * 检查输入。
             */
            if (recipe.mInputs != null) {

                for (ItemStack input : recipe.mInputs) {

                    if (matchesItem(input, query)) {
                        return true;
                    }
                }
            }

            /*
             * 检查输出。
             */
            if (recipe.mOutputs != null) {

                for (ItemStack output : recipe.mOutputs) {

                    if (matchesItem(output, query)) {
                        return true;
                    }
                }
            }

        } catch (Throwable ignored) {

            /*
             * 单个异常配方不能影响整个搜索系统。
             */
            return false;
        }

        return false;
    }

    /**
     * 判断 ItemStack 是否匹配关键词。
     */
    private static boolean matchesItem(
            ItemStack stack,
            String query) {

        if (stack == null) {
            return false;
        }

        try {

            String displayName = stack.getDisplayName();
            String unlocalizedName = stack.getUnlocalizedName();

            if (displayName != null) {

                if (displayName
                        .toLowerCase()
                        .contains(query)) {

                    return true;
                }
            }

            if (unlocalizedName != null) {

                if (unlocalizedName
                        .toLowerCase()
                        .contains(query)) {

                    return true;
                }
            }

        } catch (Throwable ignored) {
            return false;
        }

        return false;
    }

    /**
     * 将 GTRecipe 转换成 AI 可以理解的文本。
     *
     * 示例：
     *
     * EU/t=30, duration=200 ticks,
     * inputs=[Iron Ingot x1],
     * outputs=[Iron Plate x1]
     */
    public static String describe(GTRecipe recipe) {

        if (recipe == null) {
            return "null";
        }

        StringBuilder result = new StringBuilder();

        result.append("EU/t=")
                .append(recipe.mEUt);

        result.append(", duration=")
                .append(recipe.mDuration)
                .append(" ticks");

        result.append(", inputs=");

        appendItems(
                result,
                recipe.mInputs);

        result.append(", outputs=");

        appendItems(
                result,
                recipe.mOutputs);

        return result.toString();
    }

    /**
     * 将 ItemStack 数组转换成可读文本。
     */
    private static void appendItems(
            StringBuilder result,
            ItemStack[] stacks) {

        result.append('[');

        if (stacks != null) {

            boolean first = true;

            for (ItemStack stack : stacks) {

                if (stack == null) {
                    continue;
                }

                if (!first) {
                    result.append("; ");
                }

                result.append(
                        stack.getDisplayName());

                result.append(" x");

                result.append(
                        stack.stackSize);

                first = false;
            }
        }

        result.append(']');
    }
}
