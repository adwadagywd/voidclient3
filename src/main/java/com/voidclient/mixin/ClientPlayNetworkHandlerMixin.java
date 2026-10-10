package com.voidclient.mixin;

import com.voidclient.stats.Stats;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    // require = 0: gdyby nazwa metody sie zmienila, tylko TPS przestanie dzialac (reszta moda tak).
    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"), require = 0)
    private void voidclient$timeUpdate(CallbackInfo ci) {
        Stats.onTimePacket();
    }
}
