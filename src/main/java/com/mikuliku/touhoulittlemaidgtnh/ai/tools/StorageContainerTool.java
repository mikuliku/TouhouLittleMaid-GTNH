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
 * Forge 1.7.10 / GTNH nearby inventory tool.
 */
public final class StorageContainerTool implements Tool {

    @Override
    public String getName() {
        return "storage_container";
    }

    @Override
    public String getDescription() {
        return "扫描玩家附近的IInventory容器，或从附近容器安全取出指定物品到玩家背包。"
                + " 参数：{\"action\":\"scan\"} 或 {\"action\":\"take\",\"query\":\"物品名称\",\"amount\":数量}。";
    }

    @Override
    public ToolResult execute(
            final ToolContext context,
            final String argumentsJson) {

        if (!AIConfig.storageEnabled) {
            return ToolResult.failure("附近容器取料功能已在配置中关闭。");
        }

        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        final EntityPlayer player = context.getPlayer();
        final String action = extractString(argumentsJson, "action");

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            if ("take".equalsIgnoreCase(action)) {
                                String query = extractString(argumentsJson, "query");
                                int amount = extractInt(argumentsJson, "amount", 1);
                                return take(player, query, amount);
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
        Map<String, Integer> totals = new LinkedHashMap<String, Integer>();

        int nonEmptySlots = 0;

        for (IInventory inventory : inventories) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = safeGet(inventory, slot);

                if (stack == null || stack.stackSize <= 0) {
                    continue;
                }

                nonEmptySlots++;

                /*
                 * 不使用 getItemDamage()。
                 * 这里使用玩家可见的 display name 作为查询层面的键，
                 * 避免GTNH开发环境中不同class path/mapping造成的API冲突。
                 */
                String key = stack.getDisplayName();
                Integer old = totals.get(key);
                totals.put(key, (old == null ? 0 : old) + stack.stackSize);
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("附近可访问容器：")
                .append(inventories.size())
                .append(" 个；非空槽位：")
                .append(nonEmptySlots)
                .append('\n');

        if (inventories.isEmpty()) {
            result.append("没有发现使用IInventory接口的附近容器。");
            return result.toString();
        }

        int count = 0;
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            if (count >= 80) {
                result.append("……其余物品省略。");
                break;
            }

            result.append("- ")
                    .append(entry.getKey())
                    .append(" x")
                    .append(entry.getValue())
                    .append('\n');
            count++;
        }

        if (totals.isEmpty()) {
            result.append("附近容器目前没有可见物品。");
        }

        return result.toString();
    }

    private static String take(
            EntityPlayer player,
            String query,
            int requested) {

        if (query == null || query.trim().length() == 0) {
            return "取料失败：没有指定物品名称。";
        }

        if (requested <= 0) {
            return "取料失败：数量必须大于0。";
        }

        requested = Math.min(requested, AIConfig.storageMaxTake);

        List<IInventory> inventories = findInventories(player);

        ItemStack selected = null;
        int matches = 0;

        // 先找精确名称。
        for (IInventory inventory : inventories) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = safeGet(inventory, slot);

                if (stack == null || stack.stackSize <= 0) {
                    continue;
                }

                if (matchesExact(stack, query)) {
                    if (selected == null) {
                        selected = stack;
                    }
                    if (!stack.isItemEqual(selected)) {
                        matches++;
                    }
                }
            }
        }

        // 没有精确命中时，允许唯一的模糊物品。
        if (selected == null) {
            for (IInventory inventory : inventories) {
                for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                    ItemStack stack = safeGet(inventory, slot);

                    if (stack == null || stack.stackSize <= 0) {
                        continue;
                    }

                    if (matchesFuzzy(stack, query)) {
                        if (selected == null) {
                            selected = stack;
                            matches = 1;
                        } else if (!stack.isItemEqual(selected)) {
                            matches++;
                        }
                    }
                }
            }
        }

        if (selected == null) {
            return "附近容器中没有找到“" + query + "”。";
        }

        if (matches > 1) {
            return "取料失败：“" + query
                    + "”对应多个不同物品，请先使用storage_container的scan查询精确名称。";
        }

        final ItemStack selectedReference = selected;
        final String selectedName = selectedReference.getDisplayName();

        int remaining = requested;
        int moved = 0;

        for (IInventory inventory : inventories) {
            for (int slot = 0;
                    slot < inventory.getSizeInventory() && remaining > 0;
                    slot++) {

                ItemStack stack = safeGet(inventory, slot);

                if (stack == null || stack.stackSize <= 0) {
                    continue;
                }

                /*
                 * isItemEqual() 是 Forge 1.7.10 的标准 ItemStack 比较方法，
                 * 可用于区分不同 metadata 的物品，而不直接调用 getItemDamage()。
                 */
                if (!stack.isItemEqual(selectedReference)) {
                    continue;
                }

                int amount = Math.min(remaining, stack.stackSize);
                ItemStack removed = null;

                try {
                    inventory.openInventory();
                    removed = inventory.decrStackSize(slot, amount);
                    inventory.markDirty();
                } finally {
                    try {
                        inventory.closeInventory();
                    } catch (Throwable ignored) {
                    }
                }

                if (removed == null || removed.stackSize <= 0) {
                    continue;
                }

                int removedAmount = removed.stackSize;

                /*
                 * InventoryPlayer 在1.7.10可能部分接受物品。
                 * 因此根据剩余数量计算真正转移的数量。
                 */
                player.inventory.addItemStackToInventory(removed);

                int leftover = Math.max(0, removed.stackSize);
                int inserted = removedAmount - leftover;

                if (leftover > 0) {
                    restoreToSource(inventory, slot, removed);
                }

                moved += inserted;
                remaining -= inserted;

                if (inserted == 0) {
                    return "玩家背包没有空间，已停止取料。"
                            + "目前已取出 " + moved + " 个 " + selectedName + "。";
                }
            }
        }

        if (moved == 0) {
            return "没有取出任何“" + selectedName + "”；玩家背包可能已满。";
        }

        if (moved < requested) {
            return "已取出 " + moved + " 个 " + selectedName
                    + "；请求 " + requested
                    + " 个，但剩余材料或背包空间不足。";
        }

        return "已从附近容器取出 " + moved + " 个 "
                + selectedName + "，放入玩家背包。";
    }

    private static void restoreToSource(
            IInventory inventory,
            int slot,
            ItemStack leftover) {

        if (leftover == null || leftover.stackSize <= 0) {
            return;
        }

        ItemStack current = safeGet(inventory, slot);

        if (current == null) {
            inventory.setInventorySlotContents(slot, leftover);
            inventory.markDirty();
            return;
        }

        if (current.isItemEqual(leftover)
                && ItemStack.areItemStackTagsEqual(current, leftover)) {

            int limit = Math.min(
                    inventory.getInventoryStackLimit(),
                    current.getMaxStackSize());

            int free = Math.max(0, limit - current.stackSize);
            int restore = Math.min(free, leftover.stackSize);

            if (restore > 0) {
                current.stackSize += restore;
                leftover.stackSize -= restore;
                inventory.setInventorySlotContents(slot, current);
                inventory.markDirty();
            }
        }
    }

    private static List<IInventory> findInventories(EntityPlayer player) {
        List<IInventory> result = new ArrayList<IInventory>();

        if (player == null || player.worldObj == null) {
            return result;
        }

        int radius = Math.max(1, AIConfig.storageRadius);
        int centerX = (int) Math.floor(player.posX);
        int centerY = (int) Math.floor(player.posY);
        int centerZ = (int) Math.floor(player.posZ);

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = Math.max(0, centerY - radius);
                    y <= centerY + radius; y++) {

                for (int z = centerZ - radius;
                        z <= centerZ + radius; z++) {

                    TileEntity tile =
                            player.worldObj.getTileEntity(x, y, z);

                    if (!(tile instanceof IInventory)) {
                        continue;
                    }

                    IInventory inventory = (IInventory) tile;

                    if (!result.contains(inventory)) {
                        result.add(inventory);
                    }

                    if (result.size() >= AIConfig.storageMaxContainers) {
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

    private static boolean matchesExact(
            ItemStack stack,
            String query) {

        String q = query.trim();
        String display = stack.getDisplayName();
        String unlocalized = stack.getUnlocalizedName();

        return q.equalsIgnoreCase(display)
                || q.equalsIgnoreCase(unlocalized);
    }

    private static boolean matchesFuzzy(
            ItemStack stack,
            String query) {

        String q = query.toLowerCase().trim();
        String display = stack.getDisplayName();
        String unlocalized = stack.getUnlocalizedName();

        return (display != null
                && display.toLowerCase().contains(q))
                || (unlocalized != null
                && unlocalized.toLowerCase().contains(q));
    }

    private static String extractString(
            String json,
            String key) {

        if (json == null) {
            return "";
        }

        String token = "\"" + key + "\"";
        int keyPos = json.indexOf(token);

        if (keyPos < 0) {
            return "";
        }

        int colon = json.indexOf(
                ':',
                keyPos + token.length());

        if (colon < 0) {
            return "";
        }

        int firstQuote =
                json.indexOf('"', colon + 1);

        if (firstQuote < 0) {
            return "";
        }

        int secondQuote =
                json.indexOf('"', firstQuote + 1);

        if (secondQuote <= firstQuote) {
            return "";
        }

        return json.substring(
                firstQuote + 1,
                secondQuote).trim();
    }

    private static int extractInt(
            String json,
            String key,
            int fallback) {

        if (json == null) {
            return fallback;
        }

        String token = "\"" + key + "\"";
        int keyPos = json.indexOf(token);

        if (keyPos < 0) {
            return fallback;
        }

        int colon = json.indexOf(
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
