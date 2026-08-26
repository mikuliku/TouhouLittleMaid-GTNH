package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import java.util.List;

/**
 * 第10步B：普通 Forge 1.7.10 工作台配方的真实执行器。
 *
 * 安全原则：
 * - 只执行 CraftingManager 中真实存在的 IRecipe。
 * - 先检查玩家背包材料，再扣除材料。
 * - 不允许通过参数直接指定 Item ID 来创造物品。
 * - GregTech RecipeMap 不在这里伪造执行。
 */
public final class VanillaCraftExecutorTool implements Tool {

    @Override
    public String getName() {
        return "vanilla_craft_execute";
    }

    @Override
    public String getDescription() {
        return "执行已经存在于Forge 1.7.10 CraftingManager中的普通工作台配方。需要提供query和amount。不会伪造GTNH机器输出。";
    }

    @Override
    public ToolResult execute(ToolContext context, String argumentsJson) {
        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        EntityPlayer player = context.getPlayer();
        String query = extract(argumentsJson, "query");
        int amount = parseAmount(argumentsJson);

        if (query.length() == 0) {
            return ToolResult.failure("没有指定要合成的物品。");
        }

        if (amount < 1 || amount > 64) {
            return ToolResult.failure("一次最多合成64个输出。");
        }

        if (player.worldObj == null) {
            return ToolResult.failure("玩家世界不可用。");
        }

        IRecipe recipe = findRecipe(query);
        if (recipe == null) {
            return ToolResult.failure(
                    "CraftingManager中没有找到普通工作台配方。"
                            + "如果这是GTNH机器配方，请使用recipe_search查询。");
        }

        ItemStack output = recipe.getRecipeOutput();
        if (output == null) {
            return ToolResult.failure("找到的配方没有有效输出。");
        }

        int crafts = amount / Math.max(1, output.stackSize);
        if (amount % Math.max(1, output.stackSize) != 0) {
            crafts++;
        }

        int[] counts = new int[9];
        if (!collectIngredients(recipe, player.worldObj, counts)) {
            return ToolResult.failure("无法解析这个1.7.10配方的材料，因此没有执行。");
        }

        for (int slot = 0; slot < counts.length; slot++) {
            counts[slot] *= crafts;
        }

        if (!hasIngredients(player, recipe, counts)) {
            return ToolResult.failure("玩家背包中没有足够的材料，未执行合成。");
        }

        for (int i = 0; i < crafts; i++) {
            InventoryCrafting grid = buildGrid(recipe, player.worldObj);
            if (grid == null) {
                return ToolResult.failure("无法建立工作台配方矩阵，未执行合成。");
            }

            if (!consumeGridIngredients(player, grid)) {
                return ToolResult.failure("扣除材料时发现材料不足，已停止执行。");
            }

            ItemStack result = recipe.getCraftingResult(grid);
            if (result == null) {
                return ToolResult.failure("配方没有返回有效结果。");
            }

            result = result.copy();
            if (!player.inventory.addItemStackToInventory(result)) {
                player.dropPlayerItemWithRandomChoice(result, false);
            }
        }

        return ToolResult.success(
                "已完成普通工作台配方："
                        + output.getDisplayName()
                        + " × "
                        + (crafts * Math.max(1, output.stackSize))
        );
    }

    private static IRecipe findRecipe(String query) {
        String q = query.toLowerCase();
        List<?> list = CraftingManager.getInstance().getRecipeList();

        for (Object obj : list) {
            if (!(obj instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe)obj;
            ItemStack out = recipe.getRecipeOutput();
            if (out == null) continue;

            String display = out.getDisplayName();
            String unloc = out.getUnlocalizedName();

            if ((display != null && display.toLowerCase().contains(q))
                    || (unloc != null && unloc.toLowerCase().contains(q))) {
                return recipe;
            }
        }

        return null;
    }

    private static InventoryCrafting buildGrid(IRecipe recipe, World world) {
        InventoryCrafting grid = new InventoryCrafting(
                new net.minecraft.inventory.Container() {
                    @Override public boolean canInteractWith(EntityPlayer p) { return false; }
                    @Override public boolean canInteractWith(EntityPlayer p, int x, int y, int z) { return false; }
                    @Override public net.minecraft.item.ItemStack transferStackInSlot(EntityPlayer p, int i) { return null; }
                }, 3, 3);

        Object[] input = readInputs(recipe);
        if (input == null) return null;

        for (int i = 0; i < input.length && i < 9; i++) {
            if (input[i] instanceof ItemStack) {
                grid.setInventorySlotContents(i, ((ItemStack)input[i]).copy());
            }
        }

        return grid;
    }

    private static boolean collectIngredients(IRecipe recipe, World world, int[] counts) {
        Object[] input = readInputs(recipe);
        if (input == null) return false;

        for (int i = 0; i < input.length && i < 9; i++) {
            if (input[i] instanceof ItemStack) {
                counts[i] = Math.max(1, ((ItemStack)input[i]).stackSize);
            }
        }
        return true;
    }

    private static boolean hasIngredients(EntityPlayer player, IRecipe recipe, int[] counts) {
        Object[] input = readInputs(recipe);
        if (input == null) return false;

        for (int i = 0; i < input.length && i < 9; i++) {
            if (!(input[i] instanceof ItemStack)) continue;
            ItemStack need = (ItemStack)input[i];
            int required = Math.max(1, need.stackSize) * Math.max(1, counts[i] / Math.max(1, need.stackSize));
            if (countMatching(player, need) < required) return false;
        }
        return true;
    }

    private static boolean consumeGridIngredients(EntityPlayer player, InventoryCrafting grid) {
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack need = grid.getStackInSlot(i);
            if (need == null) continue;
            if (!removeMatching(player, need)) return false;
        }
        return true;
    }

    private static int countMatching(EntityPlayer player, ItemStack target) {
        int total = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (sameItem(stack, target)) total += stack.stackSize;
        }
        return total;
    }

    private static boolean removeMatching(EntityPlayer player, ItemStack target) {
        int left = Math.max
