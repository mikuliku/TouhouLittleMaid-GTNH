package com.mikuliku.touhoulittlemaidgtnh;

import com.mikuliku.touhoulittlemaidgtnh.ai.AIConfig;
import com.mikuliku.touhoulittlemaidgtnh.entity.EntityMaidJiuHu;
import com.mikuliku.touhoulittlemaidgtnh.proxy.CommonProxy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

@Mod(
        modid = TouhouLittleMaidGTNH.MODID,
        name = "Touhou Little Maid - GTNH Edition",
        version = TouhouLittleMaidGTNH.VERSION,
        acceptableRemoteVersions = "*"
)
public class TouhouLittleMaidGTNH {
    public static final String MODID = "touhoulittlemaidgtnh";
    public static final String VERSION = "0.1.0-gtnh";

    @SidedProxy(
            clientSide = "com.mikuliku.touhoulittlemaidgtnh.proxy.ClientProxy",
            serverSide = "com.mikuliku.touhoulittlemaidgtnh.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        AIConfig.load(event.getSuggestedConfigurationFile().getParentFile());
        EntityRegistry.registerModEntity(
                EntityMaidJiuHu.class,
                "maid",
                1,
                this,
                80,
                3,
                true
        );
        proxy.preInit();
    }
}
