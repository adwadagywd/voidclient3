package com.voidclient.render;

import com.voidclient.VoidConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Fullbright zaimplementowany bezpiecznie - przez zwykla opcje gry (gamma), bez zadnego
 * mixina. Gra wewnetrznie nie ogranicza wartosci gamma do 1.0 (to tylko limit suwaka w GUI),
 * wiec ustawienie jej wyzej (16) daje efekt pelnej jasnosci identyczny jak w typowych
 * "fullbright" modach, zero ryzyka crasha.
 */
public final class WorldFeatures {
    private static Double savedGamma = null;

    private WorldFeatures() {}

    public static void tick(MinecraftClient mc) {
        if (mc.options == null) return;
        boolean fb = VoidConfig.data.world.fullbright;
        if (fb) {
            if (savedGamma == null) savedGamma = mc.options.getGamma().getValue();
            if (mc.options.getGamma().getValue() < 15.9) {
                mc.options.getGamma().setValue(16.0);
            }
        } else if (savedGamma != null) {
            mc.options.getGamma().setValue(savedGamma);
            savedGamma = null;
        }
    }
}
