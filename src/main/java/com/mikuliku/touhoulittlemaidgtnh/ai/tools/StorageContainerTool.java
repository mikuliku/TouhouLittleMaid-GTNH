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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Nearby container access for the GTNH 1.7.10 port.
 *
 * Important: this class intentionally does NOT call openInventory/closeInventory.
 * The GTNH development classpath currently exposes an incompatible IInventory
 * signature during compilation. Direct slot access through decrStackSize and
 * setInventorySlotContents is sufficient for the containers targeted here.
 */
public final class StorageContainerTool implements Tool {

    @Override
    public String getName() {
        return "storage_container";
    }

    @Override
    public String getDescription() {
        return "扫描附近IInventory容器，或从附近容器取出指定物品。"
                + " 参数：{\"action\":\"scan\"} 或 "
                + "{\"action\":\"take\",\"query\":\"物品名称\",\"amount\":数量}。";
    }

    @Override
    public ToolResult execute(
            final ToolContext context,
            final String argumentsJson) {

        if (!AIConfig.storageEnabled) {
            return ToolResult.failure("附近容器取料功能已关闭。");
        }

        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        final EntityPlayer player = context.getPlayer();
        final String action = getString(argumentsJson, "action");

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            if ("take".equalsIgnoreCase(action)) {
                                return take(
                                        player,
                                        getString(argumentsJson, "query"),
                                        getInt(argumentsJson, "amount", 1));
                            }
                            return scan(player);
                        }
                    },
                    8,
                    TimeUnit.SECONDS);

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure(
                    "容器操作失败："
                            + e.getClass().getSimpleName()
                            + " "
                            + String.valueOf(e.getMessage()));
        }
    }

    private static String scan(EntityPlayer player) {
        List<IInventory> inventories = findInventories(player);
        Map<String, Integer> totals =
                new LinkedHashMap<String, Integer>();

        int slots = 0;

        for (IInventory inventory : inventories) {
            for (int slot = 0;
                    slot < inventory.getSizeInventory();
                    slot++) {

                ItemStack stack = safeGet(inventory, slot);

                if (stack == null || stack.stackSize <= 0) {
                    continue;
                }

                slots++;

                String name = stack.getDisplayName();
                Integer old = totals.get(name);

                totals.put(
                        name,
                        (old == null ? 0 : old) + stack.stackSize);
            }
        }

        StringBuilder out = new StringBuilder();

        out.append("附近发现 ")
                .append(inventories.size())
                .append(" 个容器，")
                .append(slots)
                .append(" 个非空槽位。\n");

        if (inventories.isEmpty()) {
            out.append("没有发现可访问的IInventory容器。");
            return out.toString();
        }

        int count = 0;

        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            if (count >= 80) {
                out.append("……其余物品省略。");
                break;
            }

            out.append("- ")
                    .append(entry.getKey())
                    .append(" x")
                    .append(entry.getValue())
                    .append('\n');

            count++;
        }

        if (totals.isEmpty()) {
            out.append("容器中没有物品。");
        }

        return out.toString();
    }

    private static String take(
            EntityPlayer player,
            String query,
            int requested) {

        if (query == null || query.trim().length() == 0) {
            return "取料失败：没有指定物品。";
        }

        if (requested <= 0) {
            return "取料失败：数量必须大于0。";
        }

        requested = Math.min(
                requested,
                AIConfig.storageMaxTake);

        List<IInventory> inventories =
                findInventories(player);

        ItemStack target = null;
        int distinctMatches = 0;

        /*
         * 第一轮：精确匹配显示名或 unlocalized name。
         */
        for (IInventory inventory : inventories) {
            for (int slot = 0;
                    slot < inventory.getSizeInventory();
                    slot++) {

                ItemStack stack = safeGet(inventory, slot);

                if (stack == null || stack.stackSize <= 0) {
                    continue;
                }

                if (!exactMatch(stack, query)) {
                    continue;
                }

                if (target == null) {
                    target = stack;
                    distinctMatches = 1;
                } else if (!stack.isItemEqual(target)) {
                    distinctMatches++;
                }
            }
        }

        /*
         * 第二轮：没有精确匹配时才允许唯一模糊匹配。
         */
        if (target == null) {
            for (IInventory inventory : inventories) {
                for (int slot = 0;
                        slot < inventory.getSizeInventory();
                        slot++) {

                    ItemStack stack =
                            safeGet(inventory, slot);

                    if (stack == null
                            || stack.stackSize <= 0
                            || !fuzzyMatch(stack, query)) {
                        continue;
                    }

                    if (target == null) {
                        target = stack;
                        distinctMatches = 1;
                    } else if (!stack.isItemEqual(target)) {
                        distinctMatches++;
                    }
                }
            }
        }

        if (target == null) {
            return "附近容器中没有找到“"
                    + query
                    + "”。";
        }

        if (distinctMatches > 1) {
            return "“"
                    + query
                    + "”匹配到多个不同物品，请先使用scan获得精确名称。";
        }

        String targetName = target.getDisplayName();

        int remaining = requested;
        int moved = 0;

        for (IInventory inventory : inventories) {
            for (int slot = 0;
                    slot < inventory.getSizeInventory()
                            && remaining > 0;
                    slot++) {

                ItemStack stack =
                        safeGet(inventory, slot);

                if (stack == null
                        || stack.stackSize <= 0
                        || !stack.isItemEqual(target)) {
                    continue;
                }

                int amount =
                        Math.min(remaining, stack.stackSize);

                ItemStack removed =
                        inventory.decrStackSize(slot, amount);

                if (removed == null
                        || removed.stackSize <= 0) {
                    continue;
                }

                int removedAmount =
                        removed.stackSize;

                /*
                 * InventoryPlayer.addItemStackToInventory 是1.7.10
                 * 可用的标准插入入口。
                 */
                player.inventory.addItemStackToInventory(removed);

                int leftover =
                        Math.max(0, removed.stackSize);

                int inserted =
                        removedAmount - leftover;

                if (leftover > 0) {
                    restore(
                            inventory,
                            slot,
                            removed);
                }

                if (inserted > 0) {
                    moved += inserted;
                    remaining -= inserted;
                }

                if (inserted == 0) {
                    return "玩家背包没有空间。已取出 "
                            + moved
                            + " 个 "
                            + targetName
                            + "。";
                }
            }
        }

        if (moved == 0) {
            return "没有取出物品，可能是玩家背包已满。";
        }

        if (moved < requested) {
            return "已取出 "
                    + moved
                    + " 个 "
                    + targetName
                    + "；请求 "
                    + requested
                    + " 个，但材料或背包空间不足。";
        }

        return "已从附近容器取出 "
                + moved
                + " 个 "
                + targetName
                + "。";
    }

    private static void restore(
            IInventory inventory,
            int slot,
            ItemStack leftover) {

        if (leftover == null
                || leftover.stackSize <= 0) {
            return;
        }

        ItemStack current =
                safeGet(inventory, slot);

        if (current == null) {
            inventory.setInventorySlotContents(
                    slot,
                    leftover);
            inventory.markDirty();
            return;
        }

        if (current.isItemEqual(leftover)) {
            int limit =
                    Math.min(
                            inventory.getInventoryStackLimit(),
                            current.getMaxStackSize());

            int free =
                    Math.max(
                            0,
                            limit - current.stackSize);

            int restore =
                    Math.min(
                            free,
                            leftover.stackSize);

            if (restore > 0) {
                current.stackSize += restore;
                leftover.stackSize -= restore;

                inventory.setInventorySlotContents(
                        slot,
                        current);

                inventory.markDirty();
            }
        }
    }

    private static List<IInventory> findInventories(
            EntityPlayer player) {

        List<IInventory> result =
                new ArrayList<IInventory>();

        if (player == null
                || player.worldObj == null) {
            return result;
        }

        int radius =
                Math.max(
                        1,
                        AIConfig.storageRadius);

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
                                    x,
                                    y,
                                    z);

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

    private static boolean exactMatch(
            ItemStack stack,
            String query) {

        String q = query.trim();

        return q.equalsIgnoreCase(
                stack.getDisplayName())
                || q.equalsIgnoreCase(
                stack.getUnlocalizedName());
    }

    private static boolean fuzzyMatch(
            ItemStack stack,
            String query) {

        String q =
                query.toLowerCase().trim();

        String display =
                stack.getDisplayName();

        String unlocalized =
                stack.getUnlocalizedName();

        return (display != null
                && display.toLowerCase().contains(q))
                || (unlocalized != null
                && unlocalized.toLowerCase().contains(q));
    }

    private static String getString(
            String json,
            String key) {

        if (json == null) {
            return "";
        }

        String token =
                "\"" + key + "\"";

        int keyPos =
                json.indexOf(token);

        if (keyPos < 0) {
            return "";
        }

        int colon =
                json.indexOf(
                        ':',
                        keyPos + token.length());

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

    private static int getInt(
            String json,
            String key,
            int fallback) {

        if (json == null) {
            return fallback;
        }

        String token =
                "\"" + key + "\"";

        int keyPos =
                json.indexOf(token);

        if (keyPos < 0) {
            return fallback;
        }

        int colon =
                json.indexOf(
                        ':',
                        keyPos + token.length());

        if (colon < 0) {
            return fallback;
        }

        int start = colon + 1;

        while (start < json.length()
                && Character.isWhitespace(
                        json.charAt(start))) {
            start++;
        }

        int end = start;

        while (end < json.length()
                && Character.isDigit(
                        json.charAt(end))) {
            end++;
        }

        if (end <= start) {
            return fallback;
        }

        try {
            return Integer.parseInt(
                    json.substring(start, end));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
