package dev.cpvp.mixin;

import dev.cpvp.module.impl.Modules.SafeWalkModule;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void cpvp$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (SafeWalkModule.ACTIVE) cir.setReturnValue(true);
    }
}
