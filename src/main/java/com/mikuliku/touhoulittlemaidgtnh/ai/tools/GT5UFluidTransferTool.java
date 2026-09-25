package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.AIConfig;
import com.mikuliku.touhoulittlemaidgtnh.ai.MaidMainThreadScheduler;
import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Stage 12: 将附近 IFluidHandler 中的流体搬运到 GT5U 输入流体仓。
 * 不复制 GTNH 配方逻辑，只负责满足真实 GTRecipe 的 mFluidInputs。
 */
public final class GT5UFluidTransferTool implements Tool {

    @Override
    public String getName() {
        return "gt5u_fluid_transfer";
    }

    @Override
    public String getDescription() {
        return "将附近流体容器中的指定流体转移到GT5U输入流体仓。参数：{\"x\":X,\"y\":Y,\"z\":Z,\"fluid\":\"流体名\",\"amount\":1000}。";
    }

    @Override
    public ToolResult execute(final ToolContext context, final String argumentsJson) {
        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }
        if (!AIConfig.storageEnabled) {
            return ToolResult.failure("AI存储功能已关闭。");
        }

        final EntityPlayer player = context.getPlayer();
        final int x = getInt(argumentsJson, "x", Integer.MIN_VALUE);
        final int y = getInt(argumentsJson, "y", Integer.MIN_VALUE);
        final int z = getInt(argumentsJson, "z", Integer.MIN_VALUE);
        final String fluid = getString(argumentsJson, "fluid");
        final int amount = getInt(argumentsJson, "amount", 0);

        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            return ToolResult.failure("缺少控制器坐标x/y/z。");
        }
        if (fluid.length() == 0 || amount <= 0) {
            return ToolResult.failure("流体名称或数量无效。");
        }
        if (player.worldObj.isRemote) {
            return ToolResult.failure("流体转移只能在服务器侧执行。");
        }

        try {
            String result = MaidMainThreadScheduler.callAndWait(new Callable<String>() {
                @Override
                public String call() {
                    return transfer(player, x, y, z, fluid, amount);
                }
            }, 8, TimeUnit.SECONDS);

            if (result.startsWith("OK:")) {
                return ToolResult.success(result.substring(3));
            }
            return ToolResult.failure(result);
        } catch (Exception e) {
            return ToolResult.failure("流体转移失败：" + e.getClass().getSimpleName() + " " + String.valueOf(e.getMessage()));
        }
    }

    private static String transfer(EntityPlayer player, int x, int y, int z, String fluidName, int requested) {
        TileEntity controller = player.worldObj.getTileEntity(x, y, z);
        if (controller == null) return "指定坐标没有GT5U控制器。";

        Object meta = invokeNoArg(controller, "getMetaTileEntity");
        if (meta == null) return "指定坐标不是GregTech机器控制器。";

        List<Object> hatches = getHatches(meta, "mInputHatches");
        if (hatches.isEmpty()) return "当前GT5U机器没有可用输入流体仓。";

        int moved = 0;
        List<IFluidHandler> sources = findNearbySources(player);
        for (IFluidHandler source : sources) {
            if (moved >= requested) break;
            int need = requested - moved;
            moved += moveFromSource(source, hatches, fluidName, need);
        }

        if (moved < requested) {
            return "只转移了 " + moved + " mB，仍缺少 " + (requested - moved) + " mB 的流体“" + fluidName + "”。";
        }
        return "OK:已转移 " + moved + " mB 的流体“" + fluidName + "”到GT5U输入流体仓。";
    }

    private static int moveFromSource(IFluidHandler source, List<Object> hatches, String fluidName, int need) {
        int moved = 0;
        for (ForgeDirection sourceSide : ForgeDirection.values()) {
            if (moved >= need) break;
            FluidStack simulated = source.drain(sourceSide, need - moved, false);
            if (simulated == null || simulated.amount <= 0 || !matches(simulated, fluidName)) continue;

            int accepted = fillAnyHatch(hatches, simulated);
            if (accepted <= 0) continue;

            FluidStack drained = source.drain(sourceSide, accepted, true);
            if (drained != null && drained.amount > 0) {
                moved += drained.amount;
            }
        }
        return moved;
    }

    private static int fillAnyHatch(List<Object> hatches, FluidStack stack) {
        int accepted = 0;
        for (Object hatch : hatches) {
            if (accepted >= stack.amount) break;
            if (!(hatch instanceof IFluidHandler)) continue;
            IFluidHandler target = (IFluidHandler) hatch;
            for (ForgeDirection side : ForgeDirection.values()) {
                if (accepted >= stack.amount) break;
                FluidStack part = stack.copy();
                part.amount -= accepted;
                int canFill = target.fill(side, part, false);
                if (canFill <= 0) continue;
                int actual = target.fill(side, part, true);
                accepted += Math.max(0, actual);
            }
        }
        return accepted;
    }

    private static boolean matches(FluidStack stack, String query) {
        if (stack == null || stack.getFluid() == null) return false;
        String q = query.toLowerCase();
        String name = stack.getFluid().getName();
        String localized = stack.getLocalizedName();
        return (name != null && name.toLowerCase().contains(q))
                || (localized != null && localized.toLowerCase().contains(q));
    }

    private static List<IFluidHandler> findNearbySources(EntityPlayer player) {
        List<IFluidHandler> result = new ArrayList<IFluidHandler>();
        int radius = Math.max(1, AIConfig.storageRadius);
        int x0 = (int) Math.floor(player.posX);
        int y0 = (int) Math.floor(player.posY);
        int z0 = (int) Math.floor(player.posZ);

        for (int x = x0 - radius; x <= x0 + radius; x++) {
            for (int y = Math.max(0, y0 - radius); y <= y0 + radius; y++) {
                for (int z = z0 - radius; z <= z0 + radius; z++) {
                    TileEntity tile = player.worldObj.getTileEntity(x, y, z);
                    if (!(tile instanceof IFluidHandler)) continue;
                    IFluidHandler handler = (IFluidHandler) tile;
                    if (!result.contains(handler)) result.add(handler);
                    if (result.size() >= AIConfig.storageMaxContainers) return result;
                }
            }
        }
        return result;
    }

    private static List<Object> getHatches(Object meta, String fieldName) {
        List<Object> result = new ArrayList<Object>();
        Field field = findField(meta.getClass(), fieldName);
        if (field == null) return result;
        try {
            field.setAccessible(true);
            Object value = field.get(meta);
            if (value instanceof Iterable) {
                for (Object hatch : (Iterable<?>) value) if (hatch != null) result.add(hatch);
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try { return current.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { current = current.getSuperclass(); }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String name) {
        if (target == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) { type = type.getSuperclass(); }
            catch (Throwable ignored) { return null; }
        }
        return null;
    }

    private static String getString(String json, String key) {
        if (json == null) return "";
        String token = "\"" + key + "\"";
        int p = json.indexOf(token);
        if (p < 0) return "";
        int colon = json.indexOf(':', p + token.length());
        if (colon < 0) return "";
        int first = json.indexOf('"', colon + 1);
        if (first < 0) return "";
        int second = json.indexOf('"', first + 1);
        if (second <= first) return "";
        return json.substring(first + 1, second).trim();
    }

    private static int getInt(String json, String key, int fallback) {
        if (json == null) return fallback;
        String token = "\"" + key + "\"";
        int p = json.indexOf(token);
        if (p < 0) return fallback;
        int colon = json.indexOf(':', p + token.length());
        if (colon < 0) return fallback;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end <= start) return fallback;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
