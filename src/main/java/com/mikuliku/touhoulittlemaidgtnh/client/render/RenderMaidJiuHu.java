package com.mikuliku.touhoulittlemaidgtnh.client.render;

import com.mikuliku.touhoulittlemaidgtnh.entity.EntityMaidJiuHu;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.util.ResourceLocation;

public class RenderMaidJiuHu extends RenderBiped<EntityMaidJiuHu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("touhoulittlemaidgtnh", "textures/entity/maid_jiuhu.png");

    public RenderMaidJiuHu() {
        super(new ModelBiped(0.0F), 0.35F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMaidJiuHu entity) {
        return TEXTURE;
    }
}
