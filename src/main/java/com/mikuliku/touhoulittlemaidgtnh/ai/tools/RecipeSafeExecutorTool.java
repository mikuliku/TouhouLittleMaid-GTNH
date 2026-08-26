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
 * Forge 1.7.10 安全配方执行器。
 *
 * 这一版不猜测IRecipe的内部字段，也不尝试从 recipeItems/getInput
 * 之类并非所有实现都有的方法读取材料。
 *
 * 原则：
 * 1. 先找到真实 IRecipe。
 * 2. 使用 IRecipe.matches / getCraftingResult。
 * 3. 只有玩家明确拥有配方所需材料时才允许继续。
 *
 * 注意：由于1.7.10的IRecipe接口本身没有统一的“列出所有输入材料”
 * 方法，因此本类暂时只提供“配方验证执行入口”，不会为了实现自动
 * 扣材料而猜测具体Recipe实现。
 */
public final class RecipeSafeExecutorTool implements Tool {

    @Override
    public String getName() {
        return "recipe_safe_execute";
    }

    @Override
    public String getDescription() {
        return "验证并执行Forge 1.7.10真实IRecipe；不凭空创造物品，不伪造GregTech机器输出。";
    }

    @Override
    public ToolResult execute(ToolContext context, String argumentsJson) {
        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        EntityPlayer player = context.getPlayer();
        World world = player.worldObj;

        if (world == null) {
            return ToolResult.failure("玩家世界不可用。");
        }

        String query = extract(argumentsJson, "query");
        if (query.length() == 0) {
            return ToolResult.failure("没有指定配方目标。");
        }

        IRecipe recipe = findRecipe(query);
        if (recipe == null) {
            return ToolResult.failure(
                    "没有在Forge CraftingManager中找到与“"
                            + query
                            + "”匹配的普通配方。"
            );
        }

        ItemStack output = recipe.getRecipeOutput();
        if (output == null) {
            return ToolResult.failure("找到的IRecipe没有静态输出。");
        }

        /*
         * 这里进行一次真实的IRecipe调用验证。
         * 不制造一个伪造的材料矩阵，因此不会误判复杂Recipe。
         */
        InventoryCrafting emptyGrid = createGrid();

        boolean emptyMatches;
        try {
            emptyMatches = recipe.matches(emptyGrid, world);
        } catch (Throwable t) {
            emptyMatches = false;
        }

        /*
         * 大多数普通配方在空矩阵下都应当返回false。
         * 这个检查的意义是确认IRecipe本身可以被正常调用，
         * 而不是通过反射读取未知内部字段。
         */
        if (emptyMatches) {
            return ToolResult.failure(
                    "配方实现拒绝安全执行：空材料矩阵即可匹配，"
                            + "为防止AI异常生成物品，本次未执行。"
            );
        }

        return ToolResult.failure(
                "已找到真实配方："
                        + output.getDisplayName()
                        + " × "
                        + output.stackSize
                        + "。"
                        + "但Forge 1.7.10的IRecipe接口没有统一的输入材料枚举API，"
                        + "当前安全版本不会猜测材料并扣除背包。"
                        + "请使用recipe_search查看配方；GregTech机器配方将在GT5U专用执行器中处理。"
        );
    }

    private static IRecipe findRecipe(String query) {
        String q = query.toLowerCase();
        List<?> recipes = CraftingManager.getInstance().getRecipeList();

        for (Object object : recipes) {
            if (!(object instanceof IRecipe)) {
                continue;
            }

            IRecipe recipe = (IRecipe)object;
            ItemStack output;

            try {
                output = recipe.getRecipeOutput();
            } catch (Throwable ignored) {
                continue;
            }

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

        return null;
    }

    private static InventoryCrafting createGrid() {
        return new InventoryCrafting(new net.minecraft.inventory.Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }

            @Override
            public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
                return null;
            }
        }, 3, 3);
    }

    private static String extract(String json, String key) {
        if (json == null) {
            return "";
        }

        String token = "\"" + key + "\"";
        int keyPos = json.indexOf(token);

        if (keyPos < 0) {
            return "";
        }

        int colon = json.indexOf(':', keyPos + token.length());

        if (colon < 0) {
            return "";
        }

        int firstQuote = json.indexOf('"', colon + 1);

        if (firstQuote < 0) {
            return "";
        }

        int secondQuote = json.indexOf('"', firstQuote + 1);

        if (secondQuote <= firstQuote) {
            return "";
        }

        return json.substring(firstQuote + 1, secondQuote).trim();
    }
}
