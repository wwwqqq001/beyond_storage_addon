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

@Mod.EventBusSubscriber(modid = BeyondStorageAddon.MODID, value = Dist.CLIENT)
public class ClientHandler {

    public static final KeyMapping KEY_STORE_ALL = new KeyMapping(
            "key.beyondstorage.store_all",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.beyondstorage"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY_STORE_ALL);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 只有当打开了非玩家背包的容器界面时，按键才生效
        if (KEY_STORE_ALL.consumeClick()) {
            if (Minecraft.getInstance().screen instanceof AbstractContainerScreen) {
                triggerStore();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            // 排除掉维度网络自己的界面，防止按钮重叠
            String screenClassName = screen.getClass().getSimpleName();
            if (screenClassName.contains("DimensionsNet") || screenClassName.contains("DimensionsCraft")) {
                return;
            }

            // 获取 GUI 的起始坐标
            int guiLeft = (screen.width - 176) / 2; 
            int imageWidth = 176;
            int imageHeight = 166;
            
            // 尝试从 screen 中获取实际的 xSize 和 ySize (通过反射或直接访问，如果是标准屏幕通常是 176x166)
            // 这里我们放在玩家背包上方一点，或者界面右下角
            
            event.addListener(Button.builder(Component.literal("存入网络"), b -> triggerStore())
                    .bounds(screen.width / 2 + 10, (screen.height + 166) / 2 - 25, 60, 20)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("一键将箱子物品存入维度网络 (快捷键: G)")))
                    .build());
        }
    }

    private static void triggerStore() {
        if (Minecraft.getInstance().player != null) {
            PacketHandler.INSTANCE.sendToServer(new PacketStoreAll());
        }
    }
}