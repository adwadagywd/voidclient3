package com.voidclient.mixin;

import com.voidclient.render.ColorGradeRenderer;
import com.voidclient.stats.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // Po wyrenderowaniu swiata (z reka), przed HUD i GUI -> interfejs zostaje bez zmian kolorow.
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void voidclient$afterWorld(CallbackInfo ci) {
        Stats.frame(MinecraftClient.getInstance());
        ColorGradeRenderer.apply();
    }
}
