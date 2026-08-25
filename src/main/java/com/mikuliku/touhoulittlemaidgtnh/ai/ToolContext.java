package com.mikuliku.touhoulittlemaidgtnh.ai;

import net.minecraft.entity.player.EntityPlayer;

public final class ToolContext {
    private final EntityPlayer player;

    public ToolContext(EntityPlayer player) {
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
