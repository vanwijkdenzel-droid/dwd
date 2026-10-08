package dev.cpvp.mixin;

import dev.cpvp.module.impl.Modules.ReachModule;
import dev.cpvp.module.impl.Modules.SafeWalkModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void cpvp$reach(CallbackInfoReturnable<Double> cir) {
        Object self = this;
        if (ReachModule.ACTIVE && self == MinecraftClient.getInstance().player) cir.setReturnValue(ReachModule.range());
    }

    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void cpvp$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (SafeWalkModule.ACTIVE && self == MinecraftClient.getInstance().player) cir.setReturnValue(true);
    }
}
