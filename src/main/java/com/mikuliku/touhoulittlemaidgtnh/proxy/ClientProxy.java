package com.mikuliku.touhoulittlemaidgtnh.proxy;

import com.mikuliku.touhoulittlemaidgtnh.entity.EntityMaidJiuHu;
import com.mikuliku.touhoulittlemaidgtnh.client.render.RenderMaidJiuHu;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityMaidJiuHu.class, new RenderMaidJiuHu());
    }
}
