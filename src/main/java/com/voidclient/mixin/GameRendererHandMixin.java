package com.voidclient.mixin;

import com.voidclient.VoidConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Przesuwa i skaluje widok reki/trzymanego przedmiotu (tzw. "view model").
 * Celowo bez zadnej rotacji - tylko skala i pozycja.
 * W 1.21.4 renderHand dostaje gotowa macierz (Matrix4f), nie MatrixStack.
 * require = 0: jesli mapowanie metody sie zmieni w przyszlej wersji gry,
 * tylko ta jedna funkcja przestanie dzialac, a reszta moda dziala dalej.
 */
@Mixin(GameRenderer.class)
public class GameRendererHandMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), require = 0)
    private void voidclient$viewModel(Camera camera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
        VoidConfig.ViewModelSettings v = VoidConfig.data.viewModel;
        if (!v.enabled || v.isNeutral()) return;
        matrix4f.translate(v.offsetX, v.offsetY, v.offsetZ);
        matrix4f.scale(v.scale);
    }
}
