package com.example.beyondstorage.network;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StoreLogic {

    public static void transferItemsToNetwork(ServerPlayer player, AbstractContainerMenu container) {
        // 1. 安全检查：如果打开的是玩家自己的背包，直接禁止操作
        if (container instanceof InventoryMenu) {
            return; 
        }

        // 2. 获取存储网络
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) {
            player.sendSystemMessage(Component.literal("§c未找到维度网络，请先创建！"), true); // true = 显示在ActionBar
            return;
        }

        UnifiedStorage storage = net.getUnifiedStorage();
        if (storage == null) return;

        int movedCount = 0;
        boolean networkFull = false;

        // 3. 智能遍历：只处理“非玩家背包”的槽位
        for (Slot slot : container.slots) {
            // 关键判断：如果这个槽位关联的库存是玩家自己的背包，或者是玩家的装备栏/副手，一律跳过
            if (slot.container == player.getInventory()) {
                continue;
            }

            if (slot.hasItem() && slot.mayPickup(player)) {
                ItemStack stack = slot.getItem();
                ItemStackType itemStackType = new ItemStackType(stack);
                
                // 尝试插入
                ItemStackType remaining = (UnifiedStorage.BeforeInsertHandlerReturnInfo.getEmpty().getStack() == null) ? 
                        (ItemStackType) storage.insert(itemStackType, false) :
                        (ItemStackType) storage.insert(itemStackType, false); // 简化调用，忽略旧版API差异

                // 计算实际存入了多少
                long originalCount = stack.getCount();
                long remainingCount = remaining.getStackAmount();
                
                if (remainingCount < originalCount) {
                    movedCount++;
                    // 更新槽位：如果剩下了就放回去，没剩下就设为空
                    if (remainingCount > 0) {
                        ItemStack newStack = stack.copy();
                        newStack.setCount((int) remainingCount);
                        slot.set(newStack);
                        networkFull = true; // 有剩余说明网络可能满/限额
                    } else {
                        slot.set(ItemStack.EMPTY);
                    }
                }
            }
        }

        // 4. 结果反馈 (优化为 ActionBar 提示)
        if (movedCount > 0) {
            container.broadcastChanges();
            player.sendSystemMessage(Component.literal("§a⬇ 已存入 " + movedCount + " 组物品"), true);
        } else {
            if (networkFull) {
                player.sendSystemMessage(Component.literal("§e网络已满"), true);
            } else {
                // 如果没有物品移动，保持静默，或者提示空
                player.sendSystemMessage(Component.literal("§7没有可存入的物品"), true);
            }
        }
    }
}
