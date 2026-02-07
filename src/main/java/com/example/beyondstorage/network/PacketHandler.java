package com.example.beyondstorage.network;

import com.example.beyondstorage.BeyondStorageAddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BeyondStorageAddon.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.messageBuilder(PacketStoreAll.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketStoreAll::encode)
                .decoder(PacketStoreAll::decode)
                .consumerMainThread(PacketStoreAll::handle)
                .add();
    }
}