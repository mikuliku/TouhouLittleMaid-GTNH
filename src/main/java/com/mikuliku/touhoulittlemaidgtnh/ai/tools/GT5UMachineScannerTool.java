package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import java.util.Locale;

public final class GT5UMachineScannerTool implements Tool {
    public String getName() { return "gt5u_machine_scan"; }
    public String getDescription() {
        return "Scan nearby GT5U machines and report controller type and RecipeMap without modifying them.";
    }

    public ToolResult execute(ToolContext context, String argumentsJson) {
        if (context == null || context.getPlayer() == null)
            return ToolResult.failure("No player context.");

        int radius = readRadius(argumentsJson);
        if (radius < 1) radius = 1;
        if (radius > 16) radius = 16;

        World world = context.getPlayer().worldObj;
        int cx = (int)Math.floor(context.getPlayer().posX);
        int cy = (int)Math.floor(context.getPlayer().posY);
        int cz = (int)Math.floor(context.getPlayer().posZ);

        StringBuilder result = new StringBuilder();
        int found = 0;

        for (int x=cx-radius; x<=cx+radius; x++) {
            for (int y=Math.max(0,cy-radius); y<=Math.min(255,cy+radius); y++) {
                for (int z=cz-radius; z<=cz+radius; z++) {
                    TileEntity tile = world.getTileEntity(x,y,z);
                    if (!(tile instanceof IGregTechTileEntity)) continue;

                    IGregTechTileEntity gt=(IGregTechTileEntity)tile;
                    IMetaTileEntity meta=gt.getMetaTileEntity();
                    if (meta==null) continue;

                    found++;
                    result.append(found).append(": ")
                          .append(meta.getClass().getSimpleName())
                          .append(" @ ").append(x).append(",").append(y).append(",").append(z);

                    if (meta instanceof MTEMultiBlockBase) {
                        RecipeMap<?> map=((MTEMultiBlockBase)meta).getRecipeMap();
                        result.append(", multiblock=true, recipeMap=");
                        result.append(map == null ? "unknown" : mapName(map));
                    } else {
                        result.append(", multiblock=false");
                    }
                    result.append('\n');

                    if (found>=64) {
                        result.append("scan_limit=64");
                        return ToolResult.success(result.toString());
                    }
                }
            }
        }

        if (found==0)
            return ToolResult.success("No GregTech machine controller found within radius="+radius+".");
        return ToolResult.success(result.toString());
    }

    private static int readRadius(String json) {
        if (json==null) return 8;
        String text=json.toLowerCase(Locale.ENGLISH);
        int p=text.indexOf("radius");
        if (p<0) return 8;
        int e=text.indexOf('=',p);
        if (e<0) e=text.indexOf(':',p);
        if (e<0) return 8;
        int s=e+1;
        while(s<text.length() && !Character.isDigit(text.charAt(s))) s++;
        int n=s;
        while(n<text.length() && Character.isDigit(text.charAt(n))) n++;
        try { return Integer.parseInt(text.substring(s,n)); }
        catch(Exception ignored) { return 8; }
    }

    private static String mapName(RecipeMap<?> map) {
        try {
            java.lang.reflect.Method m=map.getClass().getMethod("getUnlocalizedName");
            Object v=m.invoke(map);
            if(v!=null) return String.valueOf(v);
        } catch(Throwable ignored) {}
        return String.valueOf(map);
    }
}
