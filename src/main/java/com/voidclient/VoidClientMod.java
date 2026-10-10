package com.voidclient;

import com.voidclient.gui.VoidMenuScreen;
import com.voidclient.hud.HudManager;
import com.voidclient.stats.Stats;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VoidClientMod implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        VoidConfig.load();
        HudManager.init();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voidclient.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "category.voidclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Stats.tick(client);
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && client.world != null) {
                    client.setScreen(new VoidMenuScreen());
                }
            }
        });

        HudRenderCallback.EVENT.register(HudManager::render);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> Stats.reset());
    }
}
