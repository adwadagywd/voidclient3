package com.voidclient.mixin;

import com.voidclient.VoidConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.InGameOverlayRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No Pumpkin Blur" - anuluje rysowanie nakladki (dynie na glowie / pod woda), gdy wlaczone.
 * UWAGA: podobnie jak przy View Modelu, to eksperymentalny hak w metode gry, ktorej dokladnej
 * sygnatury nie moglem zweryfikowac offline - jesli gra nie wstanie, przeslij crash-log.
 * Ten sam generyczny mechanizm renderuje tez nakladke "pod woda", wiec przy wlaczonej opcji
 * moze zniknac rowniez ona.
 */
@Mixin(InGameOverlayRenderer.class)
public class PumpkinOverlayMixin {
    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void voidclient$noPumpkin(DrawContext context, Identifier texture, float alpha, CallbackInfo ci) {
        if (VoidConfig.data.world.noPumpkinBlur) {
            ci.cancel();
        }
    }
}
