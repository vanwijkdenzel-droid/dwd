package dev.cpvp.mixin;

import dev.cpvp.module.impl.Modules.VelocityModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    /** TAIL = main-thread pass only. */
    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"))
    private void cpvp$velocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        VelocityModule.apply(packet);
    }
}
