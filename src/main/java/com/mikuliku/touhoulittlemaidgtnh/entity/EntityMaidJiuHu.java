package com.mikuliku.touhoulittlemaidgtnh.entity;

import com.mikuliku.touhoulittlemaidgtnh.ai.MaidChatManager;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class EntityMaidJiuHu extends EntityTameable {

    private int chatCooldown;

    public EntityMaidJiuHu(World world) {
        super(world);

        this.setSize(0.6F, 1.5F);

        this.getNavigator().setAvoidsWater(true);

        this.tasks.addTask(
                0,
                new EntityAISwimming(this)
        );
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();

        this.getEntityAttribute(
                SharedMonsterAttributes.maxHealth
        ).setBaseValue(20.0D);

        this.getEntityAttribute(
                SharedMonsterAttributes.movementSpeed
        ).setBaseValue(0.30D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable entity) {
        return null;
    }

    @Override
    public boolean interact(EntityPlayer player) {

        if (!this.worldObj.isRemote
                && this.isTamed()
                && this.getOwner() == player) {

            player.addChatMessage(
                    new ChatComponentText(
                            "§d酒狐§f：主人，需要我做些什么吗？"
                    )
            );

            return true;
        }

        return super.interact(player);
    }

    /**
     * 设置酒狐的主人。
     */
    public void setOwnerPlayer(EntityPlayer player) {

        this.setTamed(true);

        this.func_152115_b(
                player.getUniqueID().toString()
        );

        this.tasks.addTask(
                2,
                new EntityAIFollowOwner(
                        this,
                        1.0D,
                        4.0F,
                        2.0F
                )
        );
    }

    /**
     * Minecraft 1.7.10 实体显示名称。
     */
    @Override
    public IChatComponent func_145748_c_() {
        return new ChatComponentText("酒狐");
    }

    /**
     * Minecraft 1.7.10 命令发送者名称。
     *
     * 这里故意不添加 @Override。
     */
    public String getCommandSenderName() {
        return "酒狐";
    }

    @Override
    public void onLivingUpdate() {

        super.onLivingUpdate();

        if (chatCooldown > 0) {
            chatCooldown--;
        }
    }

    public boolean canChat() {
        return chatCooldown <= 0;
    }

    public void setChatCooldown(int ticks) {
        chatCooldown = ticks;
    }

    /**
     * 向 AI 系统发送玩家消息。
     */
    public void askAI(
            EntityPlayer player,
            String message
    ) {

        MaidChatManager.request(
                this,
                player,
                message
        );
    }
}
