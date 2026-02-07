package com.example.beyondstorage.network;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StoreLogic {

    public static void transferItemsToNetwork(ServerPlayer player, AbstractContainerMenu container) {
        // 1. 获取玩家对应的维度网络
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);

        if (net == null) {
            player.sendSystemMessage(Component.literal("§c[Beyond Storage] 未找到您的维度网络，请先创建一个！"));
            return;
        }

        UnifiedStorage storage = net.getUnifiedStorage();
        if (storage == null) {
            player.sendSystemMessage(Component.literal("§c[Beyond Storage] 存储网络异常。"));
            return;
        }

        // 2. 确定容器中的物品槽位 (排除玩家背包)
        // 玩家背包通常占据最后 36 个槽位 (27 存储 + 9 快捷栏)
        int containerSlotsCount = container.slots.size() - 36;
        if (containerSlotsCount <= 0) return;

        boolean movedAny = false;

        for (int i = 0; i < containerSlotsCount; i++) {
            Slot slot = container.getSlot(i);
            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                
                // 将物品包装为模组定义的 ItemStackType
                ItemStackType itemStackType = new ItemStackType(stack);
                
                // 执行插入
                ItemStackType remaining = (ItemStackType) storage.insert(itemStackType, false);
                
                // 更新槽位中的物品
                slot.set(remaining.getStack());
                movedAny = true;
            }
        }

        if (movedAny) {
            container.broadcastChanges();
            player.sendSystemMessage(Component.literal("§a[Beyond Storage] 已将物品存入维度网络！"));
        } else {
            player.sendSystemMessage(Component.literal("§e[Beyond Storage] 箱子是空的，或者网络已满。"));
        }
    }
}