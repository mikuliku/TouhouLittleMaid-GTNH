package com.mikuliku.touhoulittlemaidgtnh;

import com.mikuliku.touhoulittlemaidgtnh.ai.AIConfig;
import com.mikuliku.touhoulittlemaidgtnh.ai.MaidChatEventHandler;
import com.mikuliku.touhoulittlemaidgtnh.ai.MaidMainThreadScheduler;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolRegistry;
import com.mikuliku.touhoulittlemaidgtnh.ai.tools.PlayerStatusTool;
import com.mikuliku.touhoulittlemaidgtnh.command.CommandJiuHu;
import com.mikuliku.touhoulittlemaidgtnh.entity.EntityMaidJiuHu;
import com.mikuliku.touhoulittlemaidgtnh.proxy.CommonProxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

import net.minecraftforge.common.MinecraftForge;

@Mod(
        modid = TouhouLittleMaidGTNH.MODID,
        name = "Touhou Little Maid - GTNH Edition",
        version = TouhouLittleMaidGTNH.VERSION,
        acceptableRemoteVersions = "*"
)
public class TouhouLittleMaidGTNH {

    public static final String MODID = "touhoulittlemaidgtnh";
    public static final String VERSION = "0.3.0-gtnh";

    @SidedProxy(
            clientSide = "com.mikuliku.touhoulittlemaidgtnh.proxy.ClientProxy",
            serverSide = "com.mikuliku.touhoulittlemaidgtnh.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        AIConfig.load(
                event.getSuggestedConfigurationFile().getParentFile());

        EntityRegistry.registerModEntity(
                EntityMaidJiuHu.class,
                "maid",
                1,
                this,
                80,
                3,
                true);

        ToolRegistry.register(new PlayerStatusTool());

        MinecraftForge.EVENT_BUS.register(
                new MaidChatEventHandler());

        FMLCommonHandler.instance().bus().register(
                new MaidMainThreadScheduler());

        proxy.preInit();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandJiuHu());
    }
}
