package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;
import net.minecraft.entity.player.EntityPlayer;

public final class PlayerStatusTool implements Tool {
    @Override
    public String getName() {
        return "player_status";
    }

    @Override
    public String getDescription() {
        return "查询玩家当前的世界、坐标、生命值和经验等级。当前为只读工具。";
    }

    @Override
    public ToolResult execute(ToolContext context, String argumentsJson) {
        EntityPlayer player = context.getPlayer();

        String result = "玩家=" + player.getCommandSenderName()
                + ", 世界=" + player.worldObj.provider.getDimensionName()
                + ", X=" + (int) player.posX
                + ", Y=" + (int) player.posY
                + ", Z=" + (int) player.posZ
                + ", 生命=" + player.getHealth()
                + ", 等级=" + player.experienceLevel;

        return ToolResult.success(result);
    }
}
