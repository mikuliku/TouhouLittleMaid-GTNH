package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.AIConfig;
import com.mikuliku.touhoulittlemaidgtnh.ai.MaidMainThreadScheduler;
import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 统计玩家背包 + 附近 IInventory 中某种物品的实际数量。
 *
 * 这是自动合成链的准备步骤：
 * AI 在真正执行合成前，先知道材料实际在哪里、数量是多少。
 */
public final class MaterialCheckTool implements Tool {

    @Override
    public String getName() {
        return "material_check";
    }

    @Override
    public String getDescription() {
        return "统计玩家背包和附近容器中指定物品的实际数量。"
                + " 参数：{\"query\":\"物品名称\"}。"
                + " 只读取，不修改库存。";
    }

    @Override
    public ToolResult execute(
            final ToolContext context,
            final String argumentsJson) {

        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        final EntityPlayer player = context.getPlayer();
        final String query = extract(argumentsJson, "query");

        if (query.length() == 0) {
            return ToolResult.failure("没有指定要检查的物品。");
        }

        try {
            String result =
                    MaidMainThreadScheduler.callAndWait(
                            new Callable<String>() {
                                @Override
                                public String call() {
                                    return check(player, query);
                                }
                            },
                            8,
                            TimeUnit.SECONDS);

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure(
                    "材料检查失败："
                            + e.getClass().getSimpleName()
                            + " "
                            + String.valueOf(e.getMessage()));
        }
    }

    private static String check(
            EntityPlayer player,
            String query) {

        int playerAmount = 0;
        int storageAmount = 0;
        int containers = 0;

        for (int slot = 0;
                slot < player.inventory.getSizeInventory();
                slot++) {

            ItemStack stack =
                    player.inventory.getStackInSlot(slot);

            if (matches(stack, query)) {
                playerAmount += stack.stackSize;
            }
        }

        List<IInventory> inventories =
                findInventories(player);

        for (IInventory inventory : inventories) {
            containers++;

            for (int slot = 0;
                    slot < inventory.getSizeInventory();
                    slot++) {

                ItemStack stack =
                        safeGet(inventory, slot);

                if (matches(stack, query)) {
                    storageAmount += stack.stackSize;
                }
            }
        }

        int total =
                playerAmount + storageAmount;

        return "材料检查："
                + query
                + "\n玩家背包："
                + playerAmount
                + "\n附近容器："
                + storageAmount
                + "\n合计："
                + total
                + "\n扫描容器："
                + containers;
    }

    private static boolean matches(
            ItemStack stack,
            String query) {

        if (stack == null || stack.stackSize <= 0) {
            return false;
        }

        String q =
                query.trim().toLowerCase();

        String display =
                stack.getDisplayName();

        String unlocalized =
                stack.getUnlocalizedName();

        return (display != null
                && display.toLowerCase().contains(q))
                || (unlocalized != null
                && unlocalized.toLowerCase().contains(q));
    }

    private static List<IInventory> findInventories(
            EntityPlayer player) {

        List<IInventory> result =
                new ArrayList<IInventory>();

        if (!AIConfig.storageEnabled
                || player.worldObj == null) {
            return result;
        }

        int radius =
                Math.max(1, AIConfig.storageRadius);

        int x0 =
                (int)Math.floor(player.posX);

        int y0 =
                (int)Math.floor(player.posY);

        int z0 =
                (int)Math.floor(player.posZ);

        for (int x = x0 - radius;
                x <= x0 + radius;
                x++) {

            for (int y = Math.max(0, y0 - radius);
                    y <= y0 + radius;
                    y++) {

                for (int z = z0 - radius;
                        z <= z0 + radius;
                        z++) {

                    TileEntity tile =
                            player.worldObj.getTileEntity(
                                    x, y, z);

                    if (!(tile instanceof IInventory)) {
                        continue;
                    }

                    IInventory inventory =
                            (IInventory)tile;

                    if (!result.contains(inventory)) {
                        result.add(inventory);
                    }

                    if (result.size()
                            >= AIConfig.storageMaxContainers) {
                        return result;
                    }
                }
            }
        }

        return result;
    }

    private static ItemStack safeGet(
            IInventory inventory,
            int slot) {

        try {
            return inventory.getStackInSlot(slot);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String extract(
            String json,
            String key) {

        if (json == null) {
            return "";
        }

        String token =
                "\"" + key + "\"";

        int p =
                json.indexOf(token);

        if (p < 0) {
            return "";
        }

        int colon =
                json.indexOf(
                        ':',
                        p + token.length());

        if (colon < 0) {
            return "";
        }

        int first =
                json.indexOf(
                        '"',
                        colon + 1);

        if (first < 0) {
            return "";
        }

        int second =
                json.indexOf(
                        '"',
                        first + 1);

        if (second <= first) {
            return "";
        }

        return json.substring(
                first + 1,
                second).trim();
    }
}
