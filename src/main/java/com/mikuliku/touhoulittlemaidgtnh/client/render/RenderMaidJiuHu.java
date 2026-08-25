package com.mikuliku.touhoulittlemaidgtnh.client.render;

import com.mikuliku.touhoulittlemaidgtnh.client.model.ModelMaidJiuHu;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

/**
 * 酒狐渲染器。
 *
 * 暂时使用 Minecraft 内置 Steve 纹理作为无额外二进制资源依赖的测试纹理。
 * 下一阶段可直接替换为酒狐专用 PNG。
 */
public class RenderMaidJiuHu extends RenderBiped {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/steve/steve.png");

    public RenderMaidJiuHu() {
        super(new ModelMaidJiuHu(), 0.35F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        return TEXTURE;
    }
}
