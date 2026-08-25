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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public final class RecipeSearchTool implements Tool {

    @Override
    public String getName() {
        return "recipe_search";
    }

    @Override
    public String getDescription() {
        return "查询 Minecraft/GTNH 中指定物品的真实配方。";
    }

    @Override
    public ToolResult execute(final ToolContext context, final String arguments) {
        final String query = extractQuery(arguments);

        if (query.length() == 0) {
            return ToolResult.failure("没有提供要查询的物品名称。");
        }

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            return queryRecipes(query, 8);
                        }
                    },
                    8,
                    TimeUnit.SECONDS);

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure(
                    "查询配方失败：" + e.getClass().getSimpleName()
                            + " " + String.valueOf(e.getMessage()));
        }
    }

    private static String extractQuery(String arguments) {
        if (arguments == null) {
            return "";
        }

        String value = arguments.trim();

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

    private static String queryRecipes(String query, int limit) {
        String lower = query.toLowerCase();
        StringBuilder result = new StringBuilder();
        int found = 0;

        result.append("配方查询：").append(query).append('\n');

        List<?> recipes = CraftingManager.getInstance().getRecipeList();

        for (Object object : recipes) {
            if (!(object instanceof IRecipe)) {
                continue;
            }

            IRecipe recipe = (IRecipe) object;
            ItemStack output = recipe.getRecipeOutput();

            if (!matches(output, lower)) {
                continue;
            }

            result.append(formatForgeRecipe(recipe, output)).append('\n');
            found++;

            if (found >= limit) {
                return result.toString();
            }
        }

        try {
            found = appendGregTechRecipes(
                    result, lower, limit, found);
        } catch (Throwable ignored) {
            // GregTech 不存在或 API 不兼容时，保留 Forge 配方结果。
        }

        if (found == 0) {
            result.append("没有找到匹配的已注册配方。");
        }

        return result.toString();
    }

    private static boolean matches(ItemStack stack, String query) {
        if (stack == null) {
            return false;
        }

        String display = stack.getDisplayName();
        String unlocalized = stack.getUnlocalizedName();

        return (display != null && display.toLowerCase().contains(query))
                || (unlocalized != null && unlocalized.toLowerCase().contains(query));
    }

    /*
     * Forge 1.7.10 的 IRecipe 接口没有统一的 getInput() 方法。
     * 因此这里不能直接写 recipe.getInput()。
     *
     * 1.7.10 不同配方类型把输入保存的位置不同：
     * - ShapedRecipes / ShapelessRecipes: recipeItems
     * - ShapedOreRecipe / ShapelessOreRecipe: getInput()
     *
     * 为了兼容 GTNH 中各种 1.7.10 配方，这里统一使用反射读取。
     */
    private static String formatForgeRecipe(
            IRecipe recipe,
            ItemStack output) {

        StringBuilder result = new StringBuilder();

        result.append("[Forge合成] 输出=")
                .append(stackName(output))
                .append(" x")
                .append(output.stackSize)
                .append("；输入=");

        Object inputs = readRecipeInputs(recipe);
        result.append(formatRecipeInputs(inputs));

        return result.toString();
    }

    private static Object readRecipeInputs(IRecipe recipe) {
        try {
            Method method = findMethod(
                    recipe.getClass(),
                    "getInput");

            if (method != null) {
                method.setAccessible(true);
                return method.invoke(recipe);
            }
        } catch (Throwable ignored) {
        }

        try {
            Field field = findField(
                    recipe.getClass(),
                    "recipeItems");

            if (field != null) {
                field.setAccessible(true);
                return field.get(recipe);
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static String formatRecipeInputs(Object inputs) {
        if (inputs == null) {
            return "特殊配方";
        }

        StringBuilder result = new StringBuilder();

        if (inputs instanceof ItemStack[]) {
            ItemStack[] stacks = (ItemStack[]) inputs;

            boolean first = true;
            for (ItemStack stack : stacks) {
                if (stack == null) {
                    continue;
                }

                if (!first) {
                    result.append(" + ");
                }

                result.append(stackName(stack))
                        .append(" x")
                        .append(stack.stackSize);

                first = false;
            }

            return first ? "无/特殊输入" : result.toString();
        }

        if (inputs instanceof List) {
            List<?> list = (List<?>) inputs;

            boolean first = true;

            for (Object object : list) {
                ItemStack stack = null;

                if (object instanceof ItemStack) {
                    stack = (ItemStack) object;
                } else if (object instanceof List) {
                    List<?> alternatives = (List<?>) object;

                    if (!alternatives.isEmpty()
                            && alternatives.get(0) instanceof ItemStack) {
                        stack = (ItemStack) alternatives.get(0);
                    }
                }

                if (stack == null) {
                    continue;
                }

                if (!first) {
                    result.append(" + ");
                }

                result.append(stackName(stack))
                        .append(" x")
                        .append(stack.stackSize);

                first = false;
            }

            return first ? "特殊输入" : result.toString();
        }

        if (inputs.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(inputs);
            boolean first = true;

            for (int i = 0; i < length; i++) {
                Object object =
                        java.lang.reflect.Array.get(inputs, i);

                ItemStack stack = null;

                if (object instanceof ItemStack) {
                    stack = (ItemStack) object;
                } else if (object instanceof List) {
                    List<?> alternatives = (List<?>) object;

                    if (!alternatives.isEmpty()
                            && alternatives.get(0) instanceof ItemStack) {
                        stack = (ItemStack) alternatives.get(0);
                    }
                }

                if (stack == null) {
                    continue;
                }

                if (!first) {
                    result.append(" + ");
                }

                result.append(stackName(stack))
                        .append(" x")
                        .append(stack.stackSize);

                first = false;
            }

            return first ? "特殊输入" : result.toString();
        }

        return String.valueOf(inputs);
    }

    @SuppressWarnings("unchecked")
    private static int appendGregTechRecipes(
            StringBuilder result,
            String query,
            int limit,
            int found) throws Exception {

        if (found >= limit) {
            return found;
        }

        Class<?> recipeMapClass =
                Class.forName("gregtech.api.recipe.RecipeMap");

        Field allMapsField =
                recipeMapClass.getField("ALL_RECIPE_MAPS");

        Object allMapsObject =
                allMapsField.get(null);

        if (!(allMapsObject instanceof Map)) {
            return found;
        }

        Map<Object, Object> maps =
                (Map<Object, Object>) allMapsObject;

        Method getBackend =
                recipeMapClass.getMethod("getBackend");

        for (Map.Entry<Object, Object> entry : maps.entrySet()) {
            if (found >= limit) {
                break;
            }

            Object recipeMap = entry.getValue();
            Object backend = getBackend.invoke(recipeMap);

            Method getAllRecipes =
                    findMethod(
                            backend.getClass(),
                            "getAllRecipes");

            if (getAllRecipes == null) {
                continue;
            }

            getAllRecipes.setAccessible(true);

            Object collectionObject =
                    getAllRecipes.invoke(backend);

            if (!(collectionObject instanceof Collection)) {
                continue;
            }

            Collection<?> recipes =
                    (Collection<?>) collectionObject;

            for (Object recipe : recipes) {
                if (found >= limit) {
                    break;
                }

                ItemStack[] outputs =
                        getItemStackArray(recipe, "mOutputs");

                if (!contains(outputs, query)) {
                    continue;
                }

                result.append("[GregTech/")
                        .append(String.valueOf(entry.getKey()))
                        .append("] ")
                        .append(formatGTRecipe(recipe))
                        .append('\n');

                found++;
            }
        }

        return found;
    }

    private static String formatGTRecipe(Object recipe) {
        StringBuilder result = new StringBuilder();

        result.append("输出=")
                .append(formatStacks(
                        getItemStackArray(recipe, "mOutputs")))
                .append("；输入=")
                .append(formatStacks(
                        getItemStackArray(recipe, "mInputs")));

        FluidStack[] fluids =
                getFluidStackArray(recipe, "mFluidInputs");

        if (fluids != null && fluids.length > 0) {
            result.append("；流体输入=")
                    .append(formatFluids(fluids));
        }

        Integer duration =
                getIntField(recipe, "mDuration");

        Integer eut =
                getIntField(recipe, "mEUt");

        if (duration != null) {
            result.append("；时间=")
                    .append(duration.intValue())
                    .append(" ticks");
        }

        if (eut != null) {
            result.append("；EU/t=")
                    .append(eut.intValue());
        }

        return result.toString();
    }

    private static ItemStack[] getItemStackArray(
            Object object,
            String fieldName) {

        try {
            Field field =
                    findField(object.getClass(), fieldName);

            if (field == null) {
                return null;
            }

            field.setAccessible(true);

            Object value = field.get(object);

            return value instanceof ItemStack[]
                    ? (ItemStack[]) value
                    : null;

        } catch (Throwable ignored) {
            return null;
        }
    }

    private static FluidStack[] getFluidStackArray(
            Object object,
            String fieldName) {

        try {
            Field field =
                    findField(object.getClass(), fieldName);

            if (field == null) {
                return null;
            }

            field.setAccessible(true);

            Object value = field.get(object);

            return value instanceof FluidStack[]
                    ? (FluidStack[]) value
                    : null;

        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer getIntField(
            Object object,
            String fieldName) {

        try {
            Field field =
                    findField(object.getClass(), fieldName);

            if (field == null) {
                return null;
            }

            field.setAccessible(true);

            Object value = field.get(object);

            return value instanceof Number
                    ? Integer.valueOf(
                            ((Number) value).intValue())
                    : null;

        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findField(
            Class<?> type,
            String name) {

        Class<?> current = type;

        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private static Method findMethod(
            Class<?> type,
            String name,
            Class<?>... parameterTypes) {

        Class<?> current = type;

        while (current != null) {
            try {
                return current.getDeclaredMethod(
                        name,
                        parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private static boolean contains(
            ItemStack[] stacks,
            String query) {

        if (stacks == null) {
            return false;
        }

        for (ItemStack stack : stacks) {
            if (matches(stack, query)) {
                return true;
            }
        }

        return false;
    }

    private static String formatStacks(ItemStack[] stacks) {
        if (stacks == null || stacks.length == 0) {
            return "无/特殊输入";
        }

        StringBuilder result =
                new StringBuilder();

        boolean first = true;

        for (ItemStack stack : stacks) {
            if (stack == null) {
                continue;
            }

            if (!first) {
                result.append(" + ");
            }

            result.append(stackName(stack))
                    .append(" x")
                    .append(stack.stackSize);

            first = false;
        }

        return first
                ? "无/特殊输入"
                : result.toString();
    }

    private static String formatFluids(
            FluidStack[] fluids) {

        StringBuilder result =
                new StringBuilder();

        boolean first = true;

        for (FluidStack fluid : fluids) {
            if (fluid == null
                    || fluid.getFluid() == null) {
                continue;
            }

            if (!first) {
                result.append(" + ");
            }

            result.append(fluid.getFluid().getName())
                    .append(' ')
                    .append(fluid.amount)
                    .append("mB");

            first = false;
        }

        return first ? "无" : result.toString();
    }

    private static String stackName(ItemStack stack) {
        return stack.getDisplayName()
                + " ["
                + stack.getUnlocalizedName()
                + "]";
    }
}
