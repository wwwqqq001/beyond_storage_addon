package com.example.beyondstorage.client;

import com.example.beyondstorage.BeyondStorageAddon;
import com.example.beyondstorage.network.PacketHandler;
import com.example.beyondstorage.network.PacketStoreAll;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class ClientHandler {

    // 注册快捷键，以便在设置中可见
    public static final KeyMapping KEY_STORE_ALL = new KeyMapping(
            "key.beyondstorage.store_all",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.beyondstorage"
    );

    @Mod.EventBusSubscriber(modid = BeyondStorageAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(KEY_STORE_ALL);
        }
    }

    @Mod.EventBusSubscriber(modid = BeyondStorageAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (!(mc.screen instanceof AbstractContainerScreen)) return;

            // 检查按键是否匹配
            if (KEY_STORE_ALL.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
                
                // 核心防护：检查是否有输入框正在被点击/聚焦
                if (mc.screen.getFocused() instanceof EditBox editBox && editBox.isFocused()) {
                    return; // 玩家正在输入，不触发存入
                }

                triggerStore();
                event.setCanceled(true); // 消耗掉这个事件，防止其他逻辑误触
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                // 排除模组自带的大型界面
                String screenClassName = screen.getClass().getSimpleName();
                if (screenClassName.contains("DimensionsNet") || screenClassName.contains("DimensionsCraft")) {
                    return;
                }

                // 尝试获取 GUI 的尺寸，默认 176x166
                int xSize = 176;
                int ySize = 166;
                
                // 动态计算中心位置
                int guiLeft = (screen.width - xSize) / 2;
                int guiTop = (screen.height - ySize) / 2;

                // 按钮位置：中心偏下 (挂在 GUI 底部外面)
                int btnWidth = 60;
                int btnHeight = 18;
                int btnX = guiLeft + (xSize / 2) - (btnWidth / 2);
                int btnY = guiTop + ySize + 2; 

                event.addListener(Button.builder(Component.literal("存入网络"), b -> triggerStore())
                        .bounds(btnX, btnY, btnWidth, btnHeight)
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("一键存入维度网络 (G)")))
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