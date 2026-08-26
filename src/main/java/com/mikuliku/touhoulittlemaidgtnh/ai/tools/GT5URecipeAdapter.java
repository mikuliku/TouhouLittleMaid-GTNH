package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GT5U配方只读适配器。
 *
 * 本阶段只读取GT5U真实RecipeMap，不操作机器、不扣物品、不生成物品。
 */
public final class GT5URecipeAdapter {

    private GT5URecipeAdapter() {}

    public static List<GTRecipe> find(String query) {
        List<GTRecipe> result = new ArrayList<GTRecipe>();
        if (query == null || query.trim().length() == 0) return result;

        String q = query.toLowerCase();

        RecipeMap<?>[] maps = new RecipeMap<?>[] {
                RecipeMaps.assemblerRecipes,
                RecipeMaps.mixerRecipes,
                RecipeMaps.electrolyzerRecipes,
                RecipeMaps.centrifugeRecipes,
                RecipeMaps.extractorRecipes,
                RecipeMaps.extruderRecipes,
                RecipeMaps.formingPressRecipes,
                RecipeMaps.benderRecipes,
                RecipeMaps.latheRecipes,
                RecipeMaps.wiremillRecipes,
                RecipeMaps.maceratorRecipes,
                RecipeMaps.chemicalReactorRecipes,
                RecipeMaps.chemicalBathRecipes,
                RecipeMaps.autoclaveRecipes,
                RecipeMaps.implosionRecipes,
                RecipeMaps.compressorRecipes,
                RecipeMaps.forgeHammerRecipes,
                RecipeMaps.alloySmelterRecipes
        };

        for (RecipeMap<?> map : maps) {
            if (map == null) continue;

            for (GTRecipe recipe : map.getAllRecipes()) {
                if (matches(recipe, q)) {
                    result.add(recipe);
                    if (result.size() >= 8) return result;
                }
            }
        }

        return result;
    }

    private static boolean matches(GTRecipe recipe, String query) {
        if (recipe == null) return false;

        try {
            for (ItemStack output : recipe.mOutputs) {
                if (output == null) continue;

                String display = output.getDisplayName();
                String unlocalized = output.getUnlocalizedName();

                if ((display != null && display.toLowerCase().contains(query))
                        || (unlocalized != null && unlocalized.toLowerCase().contains(query))) {
                    return true;
                }
            }

            for (ItemStack input : recipe.mInputs) {
                if (input == null) continue;

                String display = input.getDisplayName();
                String unlocalized = input.getUnlocalizedName();

                if ((display != null && display.toLowerCase().contains(query))
                        || (unlocalized != null && unlocalized.toLowerCase().contains(query))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            return false;
        }

        return false;
    }

    public static String describe(GTRecipe recipe) {
        if (recipe == null) return "null";

        StringBuilder s = new StringBuilder();

        s.append("EU/t=").append(recipe.mEUt)
                .append(", duration=").append(recipe.mDuration)
                .append(" ticks");

        s.append(", inputs=");
        appendItems(s, recipe.mInputs);

        s.append(", outputs=");
        appendItems(s, recipe.mOutputs);

        return s.toString();
    }

    private static void appendItems(StringBuilder s, ItemStack[] stacks) {
        s.append('[');

        if (stacks != null) {
            for (int i = 0; i < stacks.length; i++) {
                ItemStack stack = stacks[i];
                if (stack == null) continue;

                if (i > 0) s.append("; ");
                s.append(stack.getDisplayName())
                        .append(" x")
                        .append(stack.stackSize);
            }
        }

        s.append(']');
    }
}
