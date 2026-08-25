package com.mikuliku.touhoulittlemaidgtnh.client.render;

import com.mikuliku.touhoulittlemaidgtnh.client.model.ModelMaidJiuHu;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

public class RenderMaidJiuHu extends RenderBiped {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("touhoulittlemaidgtnh", "textures/entity/jiuhu.png");

    public RenderMaidJiuHu() {
        super(new ModelMaidJiuHu(), 0.35F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        return TEXTURE;
    }
}
