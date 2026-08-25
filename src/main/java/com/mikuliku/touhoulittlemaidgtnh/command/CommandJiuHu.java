package com.mikuliku.touhoulittlemaidgtnh.command;

import com.mikuliku.touhoulittlemaidgtnh.entity.EntityMaidJiuHu;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import java.util.List;

/**
 * 酒狐测试召唤命令。
 *
 * 用法：
 * /jiuhu
 *
 * 召唤出来的酒狐会自动认执行命令的玩家为主人。
 */
public class CommandJiuHu extends CommandBase {

    @Override
    public String getCommandName() {
        return "jiuhu";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/jiuhu";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(
            ICommandSender sender,
            String[] args) {

        EntityPlayer player;

        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (Exception e) {
            sender.addChatMessage(
                    new ChatComponentText(
                            "只有玩家可以召唤酒狐。"
                    )
            );
            return;
        }

        World world = player.worldObj;

        EntityMaidJiuHu maid =
                new EntityMaidJiuHu(world);

        maid.setPosition(
                player.posX + 1.0D,
                player.posY,
                player.posZ + 1.0D
        );

        maid.setOwnerPlayer(player);

        world.spawnEntityInWorld(maid);

        player.addChatMessage(
                new ChatComponentText(
                        "§d酒狐§f：主人，我来了。"
                )
        );
    }

    @Override
    public boolean canCommandSenderUseCommand(
            ICommandSender sender) {

        return true;
    }

    @Override
    public List addTabCompletionOptions(
            ICommandSender sender,
            String[] args) {

        return null;
    }
}
