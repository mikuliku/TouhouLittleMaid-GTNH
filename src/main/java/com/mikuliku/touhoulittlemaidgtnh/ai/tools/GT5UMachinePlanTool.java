package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

import java.util.Locale;

/**
 * GT5U 机器执行计划器。
 *
 * 第八阶段的目标不是直接“强行控制任意 GT 机器”，
 * 而是先把真实 RecipeMap -> 机器类别这一层稳定下来。
 *
 * 原因：
 * GTNH 中大量机器是多方块机器，真正的启动逻辑位于
 * 对应 MTE 的 ProcessingLogic / checkProcessing 中，
 * 不能安全地从一个通用 AI 工具里伪造调用。
 *
 * 因此本类只生成 MachinePlan：
 * - 需要哪个 RecipeMap
 * - 推测的机器类别
 * - 是否需要物品输入
 * - 是否需要流体输入
 * - EU/t
 * - 时间
 *
 * 下一阶段再把 MachinePlan 与世界中的 GT 机器/MTE 建立绑定。
 */
public final class GT5UMachinePlanTool {

    private GT5UMachinePlanTool() {
    }

    public static final class MachinePlan {

        private final String recipeMapName;
        private final String machineFamily;
        private final boolean itemInput;
        private final boolean fluidInput;
        private final long eut;
        private final int duration;

        private MachinePlan(
                String recipeMapName,
                String machineFamily,
                boolean itemInput,
                boolean fluidInput,
                long eut,
                int duration) {

            this.recipeMapName = recipeMapName;
            this.machineFamily = machineFamily;
            this.itemInput = itemInput;
            this.fluidInput = fluidInput;
            this.eut = eut;
            this.duration = duration;
        }

        public String getRecipeMapName() {
            return recipeMapName;
        }

        public String getMachineFamily() {
            return machineFamily;
        }

        public boolean hasItemInput() {
            return itemInput;
        }

        public boolean hasFluidInput() {
            return fluidInput;
        }

        public long getEUt() {
            return eut;
        }

        public int getDuration() {
            return duration;
        }

        public String describe() {

            StringBuilder builder = new StringBuilder();

            builder.append("machine=")
                    .append(machineFamily);

            builder.append(", recipeMap=")
                    .append(recipeMapName);

            builder.append(", itemInput=")
                    .append(itemInput);

            builder.append(", fluidInput=")
                    .append(fluidInput);

            builder.append(", EU/t=")
                    .append(eut);

            builder.append(", duration=")
                    .append(duration)
                    .append(" ticks");

            return builder.toString();
        }
    }

    /**
     * 根据真实 RecipeMap 创建机器执行计划。
     */
    public static MachinePlan createPlan(
            GT5URecipeAdapter.RecipeMatch match) {

        if (match == null) {
            return null;
        }

        return createPlan(
                match.getRecipeMap(),
                match.getRecipe());
    }

    public static MachinePlan createPlan(
            RecipeMap<?> map,
            GTRecipe recipe) {

        if (map == null || recipe == null) {
            return null;
        }

        String name = getMapName(map);
        String normalized = name.toLowerCase(Locale.ENGLISH);

        String family = classify(normalized);

        boolean itemInput = recipe.mInputs != null
                && recipe.mInputs.length > 0;

        boolean fluidInput = recipe.mFluidInputs != null
                && recipe.mFluidInputs.length > 0;

        return new MachinePlan(
                name,
                family,
                itemInput,
                fluidInput,
                recipe.mEUt,
                recipe.mDuration);
    }

    /**
     * 机器类别只作为“寻找机器”的提示，
     * 绝不假定某一个具体 MTE 类一定存在。
     */
    private static String classify(String name) {

        if (contains(name, "assembler")) {
            return "ASSEMBLER";
        }

        if (contains(name, "assemblyline")
                || contains(name, "assembly_line")) {
            return "ASSEMBLY_LINE";
        }

        if (contains(name, "autoclave")) {
            return "AUTOCLAVE";
        }

        if (contains(name, "blast")) {
            return "BLAST_FURNACE";
        }

        if (contains(name, "chemical")) {
            return "CHEMICAL";
        }

        if (contains(name, "centrifuge")) {
            return "CENTRIFUGE";
        }

        if (contains(name, "compress")) {
            return "COMPRESSOR";
        }

        if (contains(name, "cutter")) {
            return "CUTTER";
        }

        if (contains(name, "electroly")) {
            return "ELECTROLYZER";
        }

        if (contains(name, "extract")) {
            return "EXTRACTOR";
        }

        if (contains(name, "extruder")) {
            return "EXTRUDER";
        }

        if (contains(name, "ferment")) {
            return "FERMENTER";
        }

        if (contains(name, "fluid") && contains(name, "solid")) {
            return "FLUID_SOLIDIFIER";
        }

        if (contains(name, "forming")) {
            return "FORMING_PRESS";
        }

        if (contains(name, "forge")) {
            return "FORGE_HAMMER";
        }

        if (contains(name, "furnace")) {
            return "FURNACE";
        }

        if (contains(name, "implosion")) {
            return "IMPLOSION_COMPRESSOR";
        }

        if (contains(name, "lathe")) {
            return "LATHE";
        }

        if (contains(name, "macerat")) {
            return "MACERATOR";
        }

        if (contains(name, "mixer")) {
            return "MIXER";
        }

        if (contains(name, "packag")) {
            return "PACKAGER";
        }

        if (contains(name, "polariz")) {
            return "POLARIZER";
        }

        if (contains(name, "printer")) {
            return "PRINTER";
        }

        if (contains(name, "recycler")) {
            return "RECYCLER";
        }

        if (contains(name, "sifter")) {
            return "SIFTER";
        }

        if (contains(name, "slicer")) {
            return "SLICER";
        }

        if (contains(name, "thermal")) {
            return "THERMAL_CENTRIFUGE";
        }

        if (contains(name, "unpack")) {
            return "UNPACKER";
        }

        if (contains(name, "wiremill")
                || contains(name, "wire_mill")) {
            return "WIREMILL";
        }

        if (contains(name, "washer")) {
            return "WASHER";
        }

        return "UNKNOWN_GT_MACHINE";
    }

    private static boolean contains(String value, String part) {
        return value.indexOf(part) >= 0;
    }

    private static String getMapName(RecipeMap<?> map) {

        try {
            java.lang.reflect.Method method =
                    map.getClass().getMethod("getUnlocalizedName");

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
}
