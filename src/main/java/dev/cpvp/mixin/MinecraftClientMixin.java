package dev.cpvp.mixin;

import dev.cpvp.module.impl.Modules.BreachSwapModule;
import dev.cpvp.module.impl.Modules.FastPlaceModule;
import dev.cpvp.module.impl.Modules.HitSelectModule;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow private int itemUseCooldown;

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void cpvp$fastPlace(CallbackInfo ci) {
        if (FastPlaceModule.ACTIVE) itemUseCooldown = 0;
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void cpvp$hitSelect(CallbackInfoReturnable<Boolean> cir) {
        if (BreachSwapModule.consumeCancel() || HitSelectModule.shouldCancel()) cir.setReturnValue(false);
    }
}
