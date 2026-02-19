package com.example.beyondstorage.network;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StoreLogic {

    public static void transferItemsToNetwork(ServerPlayer player, AbstractContainerMenu container) {
        if (container instanceof InventoryMenu) {
            return; 
        }

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) {
            player.sendSystemMessage(Component.literal("§c未找到维度网络，请先创建！"), true);
            return;
        }

        UnifiedStorage storage = net.getUnifiedStorage();
        if (storage == null) return;

        int movedCount = 0;
        boolean networkFull = false;

        for (Slot slot : container.slots) {
            if (slot.container == player.getInventory()) {
                continue;
            }

            if (slot.hasItem() && slot.mayPickup(player)) {
                ItemStack stack = slot.getItem();
                ItemStackKey itemStackKey = new ItemStackKey(stack);
                
                // 修复编译错误：直接调用 insert
                KeyAmount result = storage.insert(itemStackKey, stack.getCount(), false);
                
                if (result.key() instanceof ItemStackKey) {
                    long originalCount = stack.getCount();
                    long remainingCount = result.amount();
                    
                    if (remainingCount < originalCount) {
                        movedCount++;
                        if (remainingCount > 0) {
                            ItemStack newStack = stack.copy();
                            newStack.setCount((int) remainingCount);
                            slot.set(newStack);
                            networkFull = true;
                        } else {
                            slot.set(ItemStack.EMPTY);
                        }
                    }
                }
            }
        }

        if (movedCount > 0) {
            container.broadcastChanges();
            player.sendSystemMessage(Component.literal("§a⬇ 已存入 " + movedCount + " 组物品"), true);
        } else {
            if (networkFull) {
                player.sendSystemMessage(Component.literal("§e网络已满"), true);
            } else {
                player.sendSystemMessage(Component.literal("§7没有可存入的物品"), true);
            }
        }
    }
}