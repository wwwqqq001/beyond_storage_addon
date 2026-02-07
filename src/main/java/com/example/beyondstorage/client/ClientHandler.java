package com.example.beyondstorage.client;

import com.example.beyondstorage.BeyondStorageAddon;
import com.example.beyondstorage.network.PacketHandler;
import com.example.beyondstorage.network.PacketStoreAll;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public class ClientHandler {

    // 默认按键改为 F
    public static final KeyMapping KEY_STORE_ALL = new KeyMapping(
            "key.beyondstorage.store_all",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.beyondstorage"
    );

    // 缓存反射字段，提升性能
    private static Field imageWidthField;
    private static Field imageHeightField;

    static {
        try {
            // 尝试获取 protected 的 imageWidth 和 imageHeight 字段
            // 在生产环境中这些字段名可能会被混淆，ObfuscationReflectionHelper 会处理映射
            // 1.20.1 常用映射名：imageWidth, imageHeight (MCP)
            // 如果是在运行环境，可能需要 SRG 名，但 ForgeGradle 会在编译时重映射
            imageWidthField = ObfuscationReflectionHelper.findField(AbstractContainerScreen.class, "f_97726_"); // imageWidth
            imageHeightField = ObfuscationReflectionHelper.findField(AbstractContainerScreen.class, "f_97727_"); // imageHeight
        } catch (Exception e) {
            // 如果由上面的 SRG 失败，尝试直接用名字（开发环境）
            try {
                imageWidthField = AbstractContainerScreen.class.getDeclaredField("imageWidth");
                imageWidthField.setAccessible(true);
                imageHeightField = AbstractContainerScreen.class.getDeclaredField("imageHeight");
                imageHeightField.setAccessible(true);
            } catch (Exception ignored) {}
        }
    }

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
            // 1. 只有打开的是容器界面，且不是玩家自己的背包/创造背包时，按键才有效
            if (!(mc.screen instanceof AbstractContainerScreen)) return;
            if (mc.screen instanceof InventoryScreen || mc.screen instanceof CreativeModeInventoryScreen) return;

            if (KEY_STORE_ALL.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
                if (mc.screen.getFocused() instanceof EditBox editBox && editBox.isFocused()) {
                    return; 
                }
                triggerStore();
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                // 1. 排除玩家背包和模组自身界面
                if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) return;
                
                String screenClassName = screen.getClass().getSimpleName();
                if (screenClassName.contains("DimensionsNet") || screenClassName.contains("DimensionsCraft")) {
                    return;
                }

                // 2. 动态获取 GUI 尺寸
                int xSize = 176; // 默认值
                int ySize = 166;
                try {
                    if (imageWidthField != null) xSize = imageWidthField.getInt(screen);
                    if (imageHeightField != null) ySize = imageHeightField.getInt(screen);
                } catch (Exception e) {
                    // 如果反射失败，回退到默认值
                }

                // 3. 计算位置：屏幕中心 + GUI高度的一半 (即 GUI 底部边缘)
                int guiLeft = (screen.width - xSize) / 2;
                int guiTop = (screen.height - ySize) / 2;

                int btnWidth = 60;
                int btnHeight = 18;
                int btnX = guiLeft + (xSize / 2) - (btnWidth / 2); // 居中
                int btnY = guiTop + ySize + 2; // 紧贴底部

                event.addListener(Button.builder(Component.literal("存入网络"), b -> triggerStore())
                        .bounds(btnX, btnY, btnWidth, btnHeight)
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("一键存入维度网络 (F)")))
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
