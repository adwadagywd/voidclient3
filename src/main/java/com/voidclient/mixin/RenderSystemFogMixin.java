package com.voidclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.voidclient.VoidConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * "No Fog": przechwytuje wartosci przekazywane do RenderSystem.setShaderFogStart/End i, gdy
 * wlaczone, odsuwa je daleko poza widoczny zasieg. RenderSystem to stabilne API Mojanga
 * (com.mojang.blaze3d), a nie przemapowana logika gry, wiec ryzyko zmiany sygnatury miedzy
 * wersjami jest znacznie mniejsze niz w haku na renderFirstPersonItem. Mimo to require = 0 -
 * jesli jednak nie zadziala, mod po prostu nie usunie mgly, bez wplywu na reszte.
 */
@Mixin(RenderSystem.class)
public class RenderSystemFogMixin {
    @ModifyVariable(method = "setShaderFogStart", at = @At("HEAD"), argsOnly = true, require = 0)
    private static float voidclient$fogStart(float fogStart) {
        return VoidConfig.data.world.noFog ? 1_000_000f : fogStart;
    }

    @ModifyVariable(method = "setShaderFogEnd", at = @At("HEAD"), argsOnly = true, require = 0)
    private static float voidclient$fogEnd(float fogEnd) {
        return VoidConfig.data.world.noFog ? 1_000_001f : fogEnd;
    }
}
