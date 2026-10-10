package com.voidclient.mixin;

import com.voidclient.VoidConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "View model": przesuwa i skaluje osobno trzymany w danej rece przedmiot.
 * Celowo bez zadnej rotacji - tylko translate + scale, zgodnie z zyczeniem.
 *
 * UWAGA: to eksperymentalny hak w prywatna metode gry (renderFirstPersonItem).
 * Jesli Mojang/Yarn zmieni jej sygnature, mixin moze sie nie zaladowac i gra
 * wyrzuci blad startowy - w takim wypadku wyslij crash-log, zeby to poprawic.
 * require = 0, zeby w miare mozliwosci nie wywalac calego moda z tego powodu.
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void voidclient$viewModel(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                       float swingProgress, ItemStack item, float equipProgress,
                                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                       CallbackInfo ci) {
        VoidConfig.HandSettings s = hand == Hand.MAIN_HAND
                ? VoidConfig.data.viewModel.main
                : VoidConfig.data.viewModel.off;
        if (!s.enabled || s.isNeutral()) return;
        matrices.translate(s.offsetX, s.offsetY, s.offsetZ);
        matrices.scale(s.scale, s.scale, s.scale);
    }
}
