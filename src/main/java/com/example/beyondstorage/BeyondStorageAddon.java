package com.example.beyondstorage;

import com.example.beyondstorage.network.PacketHandler;
import com.example.beyondstorage.client.ClientHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("beyondstorage")
public class BeyondStorageAddon {
    public static final String MODID = "beyondstorage";

    public BeyondStorageAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(ClientHandler.class);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        PacketHandler.register();
    }
}
