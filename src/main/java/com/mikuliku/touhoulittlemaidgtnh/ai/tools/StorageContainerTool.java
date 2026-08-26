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
 * GTNH/Forge 1.7.10 附近容器工具。
 *
 * 目前使用 1.7.10 最通用的 IInventory 接口，因此不仅支持原版箱子，
 * 也可以兼容大量使用 IInventory 的 GTNH 仓储容器。
 *
 * action=scan
 * action=take, query="物品名称", amount=数量
 */
public final class StorageContainerTool implements Tool {

    @Override
    public String getName() {
        return "storage_container";
    }

    @Override
    public String getDescription() {
        return "扫描玩家附近的IInventory容器，或从附近容器安全取出指定物品到玩家背包；支持原版箱子和大量GTNH容器。"
                + " 参数：{\"action\":\"scan\"} 或 {\"action\":\"take\",\"query\":\"物品名称\",\"amount\":数量}。";
    }

    @Override
    public ToolResult execute(
            final ToolContext context,
            final String arguments) {

        if (!AIConfig.storageEnabled) {
            return ToolResult.failure("附近容器取料功能已在配置中关闭。");
        }

        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        final EntityPlayer player = context.getPlayer();
        final String action = extractString(arguments, "action");

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            if ("take".equalsIgnoreCase(action)) {
                                String query = extractString(arguments, "query");
                                int amount = extractInt(arguments, "amount", 1);
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
        Map<String, String> names = new LinkedHashMap<String, String>();

        int nonEmptySlots = 0;

        for (IInventory inventory : inventories) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = safeGet(inventory, slot);

                if (stack == null || stack.stackSize <= 0) {
                    continue;
                }

                nonEmptySlots++;

                String key = stack.getUnlocalizedName()
                        + ":"
                        + stack.getItemDamage();

                Integer old = totals.get(key);
                totals.put(
                        key,
                        (old == null ? 0 : old) + stack.stackSize);

                if (!names.containsKey(key)) {
                    names.put(key, stack.getDisplayName());
                }
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

            String key = entry.getKey();
            result.append("- ")
                    .append(names.get(key))
                    .append(" x")
                    .append(entry.getValue())
                    .append(" [")
                    .append(key)
                    .append("]\n");

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

        requested = Math.min(
                requested,
                AIConfig.storageMaxTake);

        List<IInventory> inventories = findInventories(player);

        ItemStack exact = null;
        ItemStack fuzzy = null;
        int fuzzyMatches = 0;

        for (IInventory inventory : inventories) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = safeGet(inventory, slot);

                if (stack == null || stack.stackSize <= 0) {
                    continue;
                }

                if (matchesExact(stack, query)) {
                    exact = stack;
                    break;
                }

                if (matchesFuzzy(stack, query)) {
                    fuzzy = stack;
                    fuzzyMatches++;
                }
            }

            if (exact != null) {
                break;
            }
        }

        if (exact == null && fuzzyMatches > 1) {
            return "取料失败：“"
                    + query
                    + "”对应多个不同物品，请先使用storage_container的scan查询精确物品名称。";
        }

        if (exact == null && fuzzy == null) {
            return "附近容器中没有找到“"
                    + query
                    + "”。";
        }

        String selectedName = exact != null
                ? exact.getDisplayName()
                : fuzzy.getDisplayName();

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

                if (!selectedName.equals(stack.getDisplayName())) {
                    continue;
                }

                int amount = Math.min(
                        remaining,
                        stack.stackSize);

                ItemStack removed;

                try {
                    inventory.openInventory(player);
                    removed = inventory.decrStackSize(slot, amount);
                    inventory.markDirty();
                } finally {
                    try {
                        inventory.closeInventory(player);
                    } catch (Throwable ignored) {
                    }
                }

                if (removed == null || removed.stackSize <= 0) {
                    continue;
                }

                int removedAmount = removed.stackSize;
                boolean inserted =
                        player.inventory.addItemStackToInventory(removed);

                if (!inserted) {
                    // 1.7.10 InventoryPlayer 可能在空间不足时只部分接受。
                    // 尽量把未接受的部分放回原槽位。
                    if (removed.stackSize > 0) {
                        ItemStack current =
                                safeGet(inventory, slot);

                        if (current == null) {
                            inventory.setInventorySlotContents(
                                    slot,
                                    removed);
                        } else if (sameItem(current, removed)) {
                            int free =
                                    inventory.getInventoryStackLimit()
                                            - current.stackSize;

                            int restore =
                                    Math.min(free, removed.stackSize);

                            if (restore > 0) {
                                current.stackSize += restore;
                                removed.stackSize -= restore;
                                inventory.setInventorySlotContents(
                                        slot,
                                        current);
                            }
                        }
                    }
                }

                int accepted = removedAmount - Math.max(
                        0,
                        removed.stackSize);

                moved += accepted;
                remaining -= accepted;

                if (accepted == 0) {
                    return "背包空间不足，已停止取料。"
                            + "目前已取出 "
                            + moved
                            + " 个 "
                            + selectedName
                            + "。";
                }
            }
        }

        if (moved == 0) {
            return "没有取出任何“"
                    + selectedName
                    + "”；可能是玩家背包空间不足。";
        }

        if (moved < requested) {
            return "已从附近容器取出 "
                    + moved
                    + " 个 "
                    + selectedName
                    + "；请求 "
                    + requested
                    + " 个，但剩余材料或背包空间不足。";
        }

        return "已从附近容器取出 "
                + moved
                + " 个 "
                + selectedName
                + "，放入玩家背包。";
    }

    private static List<IInventory> findInventories(EntityPlayer player) {
        List<IInventory> result =
                new ArrayList<IInventory>();

        if (player == null || player.worldObj == null) {
            return result;
        }

        int radius = AIConfig.storageRadius;
        int centerX = (int)Math.floor(player.posX);
        int centerY = (int)Math.floor(player.posY);
        int centerZ = (int)Math.floor(player.posZ);

        for (int x = centerX - radius;
             x <= centerX + radius;
             x++) {

            for (int y = Math.max(0, centerY - radius);
                 y <= centerY + radius;
                 y++) {

                for (int z = centerZ - radius;
                     z <= centerZ + radius;
                     z++) {

                    TileEntity tile =
                            player.worldObj.getTileEntity(x, y, z);

                    if (!(tile instanceof IInventory)) {
                        continue;
                    }

                    result.add((IInventory)tile);

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

    private static boolean matchesExact(
            ItemStack stack,
            String query) {

        String q = query.trim();

        return q.equalsIgnoreCase(stack.getDisplayName())
                || q.equalsIgnoreCase(stack.getUnlocalizedName())
                || q.equalsIgnoreCase(
                        stack.getUnlocalizedName()
                                + ":"
                                + stack.getItemDamage());
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

    private static boolean sameItem(
            ItemStack a,
            ItemStack b) {

        if (a == null || b == null) {
            return false;
        }

        if (a.getItem() != b.getItem()) {
            return false;
        }

        if (a.getItemDamage() != b.getItemDamage()) {
            return false;
        }

        return ItemStack.areItemStackTagsEqual(a, b);
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
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        int end = start;

        while (end < json.length()
                && Character.isDigit(json.charAt(end))) {
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
