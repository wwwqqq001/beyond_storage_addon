package com.example.beyondstorage.client;

import com.example.beyondstorage.BeyondStorageAddon;
import com.example.beyondstorage.network.PacketHandler;
import com.example.beyondstorage.network.PacketStoreAll;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class ClientHandler {

    // 快捷键定义
    public static final KeyMapping KEY_STORE_ALL = new KeyMapping(
            "key.beyondstorage.store_all",
            KeyConflictContext.GUI, // 仅在打开GUI时冲突/生效
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.beyondstorage"
    );

    /**
     * Mod 总线事件：用于注册 (初始化阶段)
     */
    @Mod.EventBusSubscriber(modid = BeyondStorageAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(KEY_STORE_ALL);
        }
    }

    /**
     * Forge 总线事件：用于运行时监听 (游戏进行中)
     */
    @Mod.EventBusSubscriber(modid = BeyondStorageAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            // 处理按键：必须匹配我们的快捷键，且当前处于容器界面中
            if (KEY_STORE_ALL.consumeClick()) {
                if (Minecraft.getInstance().screen instanceof AbstractContainerScreen) {
                    triggerStore();
                }
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                // 排除模组自身的界面
                String screenClassName = screen.getClass().getSimpleName();
                if (screenClassName.contains("DimensionsNet") || screenClassName.contains("DimensionsCraft")) {
                    return;
                }

                // 计算 GUI 左上角位置 (基于 176 宽度标准，如果不标准可能会稍微偏移，但通常是安全的)
                int guiLeft = (screen.width - 176) / 2;
                int guiTop = (screen.height - 166) / 2;

                // 如果 screen 提供了特定的 getGuiLeft/Top 方法访问入口，这里通过反射获取会更精确
                // 但通常 AbstractContainerScreen 的子类都会遵循类似的布局逻辑
                // 我们尝试通过 access transformers 或 简单的推断。
                // 由于无法直接访问 protected 的 leftPos/topPos，我们只能基于 screen.width 推算，
                // 或者我们可以利用 screen.getGuiLeft() 如果有的话 (原生没有 public getter)。
                
                // 为了更精准，我们尝试从 event.getListenersList() 获取组件位置来推断，但这太复杂。
                // 简便方案：假设标准 176 宽。如果 Mod 界面很大，按钮可能会偏左，但这不影响功能。
                
                // 新位置：GUI 顶部标题栏右侧
                // x = guiLeft + 150 (靠近右边缘)
                // y = guiTop + 4 (标题栏高度)
                
                int btnX = guiLeft + 150;
                int btnY = guiTop + 5;

                // 修正：如果 guiLeft 计算偏小（例如大箱子界面更宽），按钮可能会浮在外面。
                // 我们可以尝试将按钮放在屏幕右下角，稍微悬浮。
                // 或者放在玩家背包标题右侧：guiTop + 图像高度(约166) - 玩家背包高度(96) - 标题高度... 比较难算。
                
                // 最终决定：放在 GUI 窗口右上角，作为一个 20x20 的小按钮
                event.addListener(Button.builder(Component.literal("⬇"), b -> triggerStore())
                        .bounds(btnX, btnY, 20, 20) // 20x20 小按钮
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("存入网络 (G)")))
                        .build());
            }
        }
    }

    private static void triggerStore() {
        if (Minecraft.getInstance().player != null) {
            PacketHandler.INSTANCE.sendToServer(new PacketStoreAll());
        }
    }
}
