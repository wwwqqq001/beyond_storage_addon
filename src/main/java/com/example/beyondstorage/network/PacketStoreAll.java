package com.example.beyondstorage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketStoreAll {

    public PacketStoreAll() {}

    public static void encode(PacketStoreAll msg, FriendlyByteBuf buf) {}

    public static PacketStoreAll decode(FriendlyByteBuf buf) {
        return new PacketStoreAll();
    }

    public static void handle(PacketStoreAll msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                handleStore(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleStore(ServerPlayer player) {
        AbstractContainerMenu container = player.containerMenu;
        if (container == null) return;

        // Try to find the storage network capability/item in player's inventory
        StoreLogic.transferItemsToNetwork(player, container);
    }
}
