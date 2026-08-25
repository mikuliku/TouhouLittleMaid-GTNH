package com.mikuliku.touhoulittlemaidgtnh.ai;

import com.mikuliku.touhoulittlemaidgtnh.entity.EntityMaidJiuHu;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.event.ServerChatEvent;

import java.util.List;

public final class MaidChatEventHandler {

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (event.player == null || event.message == null) {
            return;
        }

        String message = event.message.trim();
        String prefix = AIConfig.chatPrefix;

        if (AIConfig.requireChatPrefix) {
            if (!message.startsWith(prefix)) {
                return;
            }

            message = message.substring(prefix.length()).trim();

            while (message.startsWith("，")
                    || message.startsWith(",")
                    || message.startsWith("：")
                    || message.startsWith(":")
                    || message.startsWith(" ")) {
                message = message.substring(1).trim();
            }
        }

        if (message.length() == 0) {
            event.player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                            "§d酒狐§f：主人想和我聊些什么呢？"));
            event.setCanceled(true);
            return;
        }

        if (!AIConfig.enabled) {
            event.player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                            "§d酒狐§f：AI 功能还没有开启哦，请先检查配置文件。"));
            event.setCanceled(true);
            return;
        }

        EntityMaidJiuHu maid = findNearestMaid(event.player);

        if (maid == null) {
            event.player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                            "§d酒狐§f：主人，请先把我召唤到附近再和我聊天哦。"));
            event.setCanceled(true);
            return;
        }

        event.setCanceled(true);
        maid.askAI(event.player, message);
    }

    private EntityMaidJiuHu findNearestMaid(EntityPlayer player) {
        double range = AIConfig.chatRange;

        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                player.posX - range,
                player.posY - range,
                player.posZ - range,
                player.posX + range,
                player.posY + range,
                player.posZ + range
        );

        List<EntityMaidJiuHu> maids =
                player.worldObj.getEntitiesWithinAABB(
                        EntityMaidJiuHu.class, box);

        EntityMaidJiuHu nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (EntityMaidJiuHu maid : maids) {
            EntityLivingBase owner = maid.getOwner();

            if (owner != player) {
                continue;
            }

            double distance = maid.getDistanceSqToEntity(player);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = maid;
            }
        }

        return nearest;
    }
}
