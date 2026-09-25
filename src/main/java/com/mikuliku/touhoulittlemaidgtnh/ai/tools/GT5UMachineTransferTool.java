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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * GT5U 输入总线材料转移工具。
 *
 * 这一阶段只负责把玩家背包或附近容器中的材料送进
 * 已存在且可访问的 GT5U 多方块输入总线。
 *
 * 不主动启动机器，也不直接调用 checkProcessing()。
 *
 * 参数：
 * {"x":100,"y":64,"z":200,"query":"物品名称","amount":16}
 *
 * x/y/z 使用 GT5UMachineScannerTool 返回的控制器坐标。
 */
public final class GT5UMachineTransferTool implements Tool {

    @Override
    public String getName() {
        return "gt5u_machine_transfer";
    }

    @Override
    public String getDescription() {
        return "把玩家背包或附近容器中的指定物品转移到指定GT5U控制器的输入总线。"
                + "只转移物品，不启动机器。参数："
                + "{\"x\":X,\"y\":Y,\"z\":Z,\"query\":\"物品名称\",\"amount\":数量}。";
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
        final int x = getInt(argumentsJson, "x", Integer.MIN_VALUE);
        final int y = getInt(argumentsJson, "y", Integer.MIN_VALUE);
        final int z = getInt(argumentsJson, "z", Integer.MIN_VALUE);
        final String query = getString(argumentsJson, "query");
        final int amount = getInt(argumentsJson, "amount", 1);

        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            return ToolResult.failure("缺少控制器坐标x/y/z。请先使用gt5u_machine_scan。");
        }

        if (query.length() == 0) {
            return ToolResult.failure("没有指定要转移的物品。");
        }

        if (amount <= 0) {
            return ToolResult.failure("转移数量必须大于0。");
        }

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            return transfer(player, x, y, z, query, amount);
                        }
                    },
                    8,
                    TimeUnit.SECONDS);

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure(
                    "GT5U材料转移失败："
                            + e.getClass().getSimpleName()
                            + " "
                            + String.valueOf(e.getMessage()));
        }
    }

    private static String transfer(
            EntityPlayer player,
            int x,
            int y,
            int z,
            String query,
            int requested) {

        TileEntity controller = player.worldObj.getTileEntity(x, y, z);
        if (controller == null) {
            return "指定坐标没有TileEntity。";
        }

        Object meta = invokeNoArg(controller, "getMetaTileEntity");
        if (meta == null) {
            return "指定坐标不是GregTech机器控制器。";
        }

        List<Object> buses = findInputBusses(meta);
        if (buses.isEmpty()) {
            return "控制器存在，但没有找到可访问的GT5U输入总线。"
                    + "请确认多方块已经成型且控制器坐标正确。";
        }

        int capacity = 0;
        for (Object bus : buses) {
            capacity += countFreeCapacity(bus, query);
        }

        if (capacity <= 0) {
            return "输入总线没有可用于“" + query + "”的空位。";
        }

        int target = Math.min(requested, capacity);
        int moved = 0;

        moved += moveFromInventory(
                player.inventory,
                buses,
                query,
                target - moved);

        if (moved < target) {
            List<IInventory> sources = findNearbyInventories(player);
            for (IInventory source : sources) {
                if (moved >= target) {
                    break;
                }
                if (source == player.inventory) {
                    continue;
                }

                moved += moveFromInventory(
                        source,
                        buses,
                        query,
                        target - moved);
            }
        }

        if (moved == 0) {
            return "没有找到可转移的“" + query + "”。";
        }

        if (moved < requested) {
            return "已转移 " + moved + " 个“" + query
                    + "”到GT5U输入总线；请求 " + requested
                    + " 个，但材料或输入总线容量不足。";
        }

        return "已转移 " + moved + " 个“" + query
                + "”到GT5U输入总线。机器尚未启动。";
    }

    private static int moveFromInventory(
            IInventory source,
            List<Object> buses,
            String query,
            int requested) {

        if (source == null || requested <= 0) {
            return 0;
        }

        int moved = 0;

        for (int slot = 0;
                slot < source.getSizeInventory() && moved < requested;
                slot++) {

            ItemStack stack = safeGet(source, slot);
            if (!matches(stack, query)) {
                continue;
            }

            int available = Math.min(
                    stack.stackSize,
                    requested - moved);

            ItemStack removed = source.decrStackSize(slot, available);
            if (removed == null || removed.stackSize <= 0) {
                continue;
            }

            int inserted = insertIntoBusses(buses, removed);

            if (inserted < removed.stackSize) {
                ItemStack leftover = removed.copy();
                leftover.stackSize -= Math.max(0, inserted);
                restore(source, slot, leftover);
            }

            moved += inserted;
        }

        if (moved > 0) {
            source.markDirty();
        }

        return moved;
    }

    private static int insertIntoBusses(
            List<Object> buses,
            ItemStack stack) {

        if (stack == null || stack.stackSize <= 0) {
            return 0;
        }

        int remaining = stack.stackSize;
        int inserted = 0;

        for (Object bus : buses) {
            if (remaining <= 0) {
                break;
            }

            int size = getSize(bus);
            if (size <= 0) {
                continue;
            }

            int circuit = getCircuitSlot(bus);

            // 先合并已有同类堆。
            for (int slot = 0; slot < size && remaining > 0; slot++) {
                if (slot == circuit) {
                    continue;
                }

                ItemStack current = safeGet(bus, slot);
                if (!sameItem(current, stack)) {
                    continue;
                }

                int limit = getStackLimit(bus, current);
                int free = Math.max(0, limit - current.stackSize);
                int add = Math.min(free, remaining);

                if (add <= 0) {
                    continue;
                }

                current.stackSize += add;
                setSlot(bus, slot, current);
                remaining -= add;
                inserted += add;
            }

            // 再使用空槽。
            for (int slot = 0; slot < size && remaining > 0; slot++) {
                if (slot == circuit) {
                    continue;
                }

                ItemStack current = safeGet(bus, slot);
                if (current != null) {
                    continue;
                }

                int limit = getStackLimit(bus, stack);
                int add = Math.min(limit, remaining);

                ItemStack placed = stack.copy();
                placed.stackSize = add;

                setSlot(bus, slot, placed);
                remaining -= add;
                inserted += add;
            }

            invokeNoArg(bus, "updateSlots");
        }

        return inserted;
    }

    private static int countFreeCapacity(Object bus, String query) {
        int size = getSize(bus);
        int circuit = getCircuitSlot(bus);
        int capacity = 0;

        for (int slot = 0; slot < size; slot++) {
            if (slot == circuit) {
                continue;
            }

            ItemStack current = safeGet(bus, slot);

            if (current == null) {
                capacity += 64;
            } else if (matches(current, query)) {
                capacity += Math.max(
                        0,
                        getStackLimit(bus, current) - current.stackSize);
            }
        }

        return capacity;
    }

    private static List<Object> findInputBusses(Object controller) {
        List<Object> result = new ArrayList<Object>();

        Field field = findField(controller.getClass(), "mInputBusses");
        if (field == null) {
            return result;
        }

        try {
            field.setAccessible(true);
            Object value = field.get(controller);

            if (!(value instanceof Iterable)) {
                return result;
            }

            for (Object bus : (Iterable<?>) value) {
                if (bus != null) {
                    result.add(bus);
                }
            }
        } catch (Throwable ignored) {
        }

        return result;
    }

    private static List<IInventory> findNearbyInventories(
            EntityPlayer player) {

        List<IInventory> result = new ArrayList<IInventory>();

        int radius = Math.max(1, AIConfig.storageRadius);
        int x0 = (int) Math.floor(player.posX);
        int y0 = (int) Math.floor(player.posY);
        int z0 = (int) Math.floor(player.posZ);

        for (int x = x0 - radius; x <= x0 + radius; x++) {
            for (int y = Math.max(0, y0 - radius);
                    y <= y0 + radius;
                    y++) {
                for (int z = z0 - radius; z <= z0 + radius; z++) {

                    TileEntity tile =
                            player.worldObj.getTileEntity(x, y, z);

                    if (!(tile instanceof IInventory)) {
                        continue;
                    }

                    IInventory inventory = (IInventory) tile;

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

    private static void restore(
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

        if (sameItem(current, leftover)) {
            int limit = Math.min(
                    inventory.getInventoryStackLimit(),
                    current.getMaxStackSize());

            int free = Math.max(0, limit - current.stackSize);
            int add = Math.min(free, leftover.stackSize);

            if (add > 0) {
                current.stackSize += add;
                leftover.stackSize -= add;
                inventory.setInventorySlotContents(slot, current);
                inventory.markDirty();
            }
        }
    }

    private static ItemStack safeGet(Object inventory, int slot) {
        if (inventory instanceof IInventory) {
            try {
                return ((IInventory) inventory).getStackInSlot(slot);
            } catch (Throwable ignored) {
            }
        }

        Object value = invokeOneArg(
                inventory,
                "getStackInSlot",
                Integer.valueOf(slot));

        return value instanceof ItemStack
                ? (ItemStack) value
                : null;
    }

    private static void setSlot(
            Object inventory,
            int slot,
            ItemStack stack) {

        if (inventory instanceof IInventory) {
            ((IInventory) inventory).setInventorySlotContents(slot, stack);
            return;
        }

        invokeTwoArgs(
                inventory,
                "setInventorySlotContents",
                Integer.valueOf(slot),
                stack);
    }

    private static int getSize(Object inventory) {
        if (inventory instanceof IInventory) {
            return ((IInventory) inventory).getSizeInventory();
        }

        Object value = invokeNoArg(
                inventory,
                "getSizeInventory");

        return value instanceof Number
                ? ((Number) value).intValue()
                : 0;
    }

    private static int getStackLimit(
            Object inventory,
            ItemStack stack) {

        int limit = 64;

        if (inventory instanceof IInventory) {
            limit = ((IInventory) inventory)
                    .getInventoryStackLimit();
        } else {
            Object value = invokeNoArg(
                    inventory,
                    "getInventoryStackLimit");

            if (value instanceof Number) {
                limit = ((Number) value).intValue();
            }
        }

        if (stack != null) {
            limit = Math.min(
                    limit,
                    stack.getMaxStackSize());
        }

        return Math.max(1, limit);
    }

    private static int getCircuitSlot(Object bus) {
        Object value = invokeNoArg(bus, "getCircuitSlot");

        return value instanceof Number
                ? ((Number) value).intValue()
                : -1;
    }

    private static boolean sameItem(
            ItemStack a,
            ItemStack b) {

        return a != null
                && b != null
                && a.isItemEqual(b)
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static boolean matches(
            ItemStack stack,
            String query) {

        if (stack == null || stack.stackSize <= 0) {
            return false;
        }

        String q = query.trim().toLowerCase();

        String display = stack.getDisplayName();
        String unlocalized = stack.getUnlocalizedName();

        return (display != null
                && display.toLowerCase().contains(q))
                || (unlocalized != null
                && unlocalized.toLowerCase().contains(q));
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

    private static Object invokeNoArg(
            Object target,
            String name) {

        if (target == null) {
            return null;
        }

        Class<?> type = target.getClass();

        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        try {
            return target.getClass()
                    .getMethod(name)
                    .invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeOneArg(
            Object target,
            String name,
            Object arg) {

        try {
            return target.getClass()
                    .getMethod(name, int.class)
                    .invoke(target, arg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeTwoArgs(
            Object target,
            String name,
            Object arg1,
            Object arg2) {

        try {
            return target.getClass()
                    .getMethod(
                            name,
                            int.class,
                            ItemStack.class)
                    .invoke(target, arg1, arg2);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getString(
            String json,
            String key) {

        if (json == null) {
            return "";
        }

        String token = "\"" + key + "\"";
        int p = json.indexOf(token);

        if (p < 0) {
            return "";
        }

        int colon = json.indexOf(
                ':',
                p + token.length());

        if (colon < 0) {
            return "";
        }

        int first = json.indexOf(
                '"',
                colon + 1);

        if (first < 0) {
            return "";
        }

        int second = json.indexOf(
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

        String token = "\"" + key + "\"";
        int p = json.indexOf(token);

        if (p < 0) {
            return fallback;
        }

        int colon = json.indexOf(
                ':',
                p + token.length());

        if (colon < 0) {
            return fallback;
        }

        int start = colon + 1;

        while (start < json.length()
                && Character.isWhitespace(
                        json.charAt(start))) {
            start++;
        }

        boolean negative =
                start < json.length()
                        && json.charAt(start) == '-';

        if (negative) {
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
            int value = Integer.parseInt(
                    json.substring(start, end));

            return negative ? -value : value;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
