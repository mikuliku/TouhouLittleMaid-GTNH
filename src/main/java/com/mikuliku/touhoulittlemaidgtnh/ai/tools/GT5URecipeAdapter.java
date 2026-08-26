package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GT5U 配方只读适配器。
 *
 * 第八阶段：
 * 1. 搜索真实 GTRecipe
 * 2. 同时保留 RecipeMap 来源
 * 3. 为后续机器执行层提供稳定的 RecipeMatch
 *
 * 本类仍然只读，不直接操作 GT 机器。
 */
public final class GT5URecipeAdapter {

    private GT5URecipeAdapter() {
    }

    /**
     * 一个真实 GT5U 配方以及它所属的 RecipeMap。
     */
    public static final class RecipeMatch {

        private final RecipeMap<?> recipeMap;
        private final GTRecipe recipe;

        public RecipeMatch(RecipeMap<?> recipeMap, GTRecipe recipe) {
            this.recipeMap = recipeMap;
            this.recipe = recipe;
        }

        public RecipeMap<?> getRecipeMap() {
            return recipeMap;
        }

        public GTRecipe getRecipe() {
            return recipe;
        }

        public String getRecipeMapName() {
            return getMapName(recipeMap);
        }

        public String describe() {
            return "[" + getRecipeMapName() + "] " + GT5URecipeAdapter.describe(recipe);
        }
    }

    /**
     * 保留旧接口，避免已有调用代码需要同时修改。
     */
    public static List<GTRecipe> find(String query) {
        List<RecipeMatch> matches = findMatches(query);
        List<GTRecipe> result = new ArrayList<GTRecipe>();

        for (RecipeMatch match : matches) {
            result.add(match.getRecipe());
        }

        return result;
    }

    /**
     * 搜索配方并保留 RecipeMap 来源。
     *
     * 这一步很重要，因为 GTNH 中“同一个物品的配方”
     * 可能分别属于压缩、装配、化学、流体固化等不同机器。
     */
    public static List<RecipeMatch> findMatches(String query) {

        List<RecipeMatch> result = new ArrayList<RecipeMatch>();

        if (query == null) {
            return result;
        }

        String trimmed = query.trim();

        if (trimmed.length() == 0) {
            return result;
        }

        String search = trimmed.toLowerCase();

        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {

            if (map == null) {
                continue;
            }

            Collection<GTRecipe> recipes;

            try {
                recipes = map.getAllRecipes();
            } catch (Throwable ignored) {
                continue;
            }

            if (recipes == null) {
                continue;
            }

            for (GTRecipe recipe : recipes) {

                if (recipe == null) {
                    continue;
                }

                if (!matches(recipe, search)) {
                    continue;
                }

                result.add(new RecipeMatch(map, recipe));

                if (result.size() >= 8) {
                    return result;
                }
            }
        }

        return result;
    }

    private static boolean matches(GTRecipe recipe, String query) {

        try {

            if (recipe.mInputs != null) {
                for (ItemStack input : recipe.mInputs) {
                    if (matchesItem(input, query)) {
                        return true;
                    }
                }
            }

            if (recipe.mOutputs != null) {
                for (ItemStack output : recipe.mOutputs) {
                    if (matchesItem(output, query)) {
                        return true;
                    }
                }
            }

        } catch (Throwable ignored) {
            return false;
        }

        return false;
    }

    private static boolean matchesItem(ItemStack stack, String query) {

        if (stack == null) {
            return false;
        }

        try {

            String displayName = stack.getDisplayName();
            String unlocalizedName = stack.getUnlocalizedName();

            if (displayName != null
                    && displayName.toLowerCase().contains(query)) {
                return true;
            }

            if (unlocalizedName != null
                    && unlocalizedName.toLowerCase().contains(query)) {
                return true;
            }

        } catch (Throwable ignored) {
            return false;
        }

        return false;
    }

    /**
     * 尽量取得 RecipeMap 的稳定名称。
     *
     * 使用反射是为了避免不同 GT5U 构建中 RecipeMap
     * 名称访问方法发生差异而导致本模组直接编译失败。
     */
    private static String getMapName(RecipeMap<?> map) {

        if (map == null) {
            return "unknown";
        }

        try {
            Method method = map.getClass().getMethod("getUnlocalizedName");
            Object value = method.invoke(map);

            if (value != null) {
                return String.valueOf(value);
            }
        } catch (Throwable ignored) {
        }

        try {
            return String.valueOf(map);
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    /**
     * 将 GTRecipe 转换成 AI 可以理解的文本。
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
        appendItems(result, recipe.mInputs);

        result.append(", outputs=");
        appendItems(result, recipe.mOutputs);

        return result.toString();
    }

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

                result.append(stack.getDisplayName());
                result.append(" x");
                result.append(stack.stackSize);

                first = false;
            }
        }

        result.append(']');
    }
}
