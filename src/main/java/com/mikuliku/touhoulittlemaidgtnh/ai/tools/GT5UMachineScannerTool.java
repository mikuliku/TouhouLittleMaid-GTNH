package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import java.lang.reflect.Method;
import java.util.Locale;

public final class GT5UMachineScannerTool implements Tool {
    public String getName() { return "gt5u_machine_scan"; }
    public String getDescription() {
        return "Scan nearby GT5U machines and report controller type and RecipeMap without modifying them.";
    }

    public ToolResult execute(ToolContext context, String argumentsJson) {
        if (context == null || context.getPlayer() == null) return ToolResult.failure("No player context.");

        int radius = readRadius(argumentsJson);
        if (radius < 1) radius = 1;
        if (radius > 16) radius = 16;

        World world = context.getPlayer().worldObj;
        int cx = (int)Math.floor(context.getPlayer().posX);
        int cy = (int)Math.floor(context.getPlayer().posY);
        int cz = (int)Math.floor(context.getPlayer().posZ);

        StringBuilder result = new StringBuilder();
        int found = 0;

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = Math.max(0, cy - radius); y <= Math.min(255, cy + radius); y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    TileEntity tile = world.getTileEntity(x, y, z);
                    if (tile == null) continue;

                    Object meta = invokeNoArg(tile, "getMetaTileEntity");
                    if (meta == null) continue;

                    String metaClass = meta.getClass().getName();
                    // Use reflection instead of direct GT5U interfaces. Some GT5U interfaces
                    // reference optional GTNHLib/ModularUI/AE2 classes which are not present
                    // on this project's compile classpath.
                    if (metaClass.indexOf("gregtech.") != 0
                            && metaClass.indexOf("com.github.GTNewHorizons.") != 0) continue;

                    found++;
                    result.append(found).append(": ")
                          .append(meta.getClass().getSimpleName())
                          .append(" @ ").append(x).append(",").append(y).append(",").append(z);

                    Object recipeMap = invokeNoArg(meta, "getRecipeMap");
                    if (recipeMap != null) {
                        result.append(", multiblock=true, recipeMap=")
                              .append(recipeMapName(recipeMap));
                    } else {
                        result.append(", multiblock=").append(isMultiBlock(meta));
                    }
                    result.append('\n');

                    if (found >= 64) {
                        result.append("scan_limit=64");
                        return ToolResult.success(result.toString());
                    }
                }
            }
        }

        if (found == 0) return ToolResult.success("No GregTech machine controller found within radius=" + radius + ".");
        return ToolResult.success(result.toString());
    }

    private static boolean isMultiBlock(Object meta) {
        Class<?> type = meta.getClass();
        while (type != null) {
            String name = type.getName();
            if (name.indexOf("MTEMultiBlockBase") >= 0 || name.indexOf("MultiBlock") >= 0) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String recipeMapName(Object map) {
        Object value = invokeNoArg(map, "getUnlocalizedName");
        if (value != null) return String.valueOf(value);
        value = invokeNoArg(map, "getLocalizedName");
        if (value != null) return String.valueOf(value);
        return map.getClass().getSimpleName();
    }

    private static int readRadius(String json) {
        if (json == null) return 8;
        String text = json.toLowerCase(Locale.ENGLISH);
        int p = text.indexOf("radius");
        if (p < 0) return 8;
        int e = text.indexOf('=', p);
        if (e < 0) e = text.indexOf(':', p);
        if (e < 0) return 8;
        int s = e + 1;
        while (s < text.length() && !Character.isDigit(text.charAt(s))) s++;
        int n = s;
        while (n < text.length() && Character.isDigit(text.charAt(n))) n++;
        if (s == n) return 8;
        try { return Integer.parseInt(text.substring(s, n)); }
        catch (Exception ignored) { return 8; }
    }
}
