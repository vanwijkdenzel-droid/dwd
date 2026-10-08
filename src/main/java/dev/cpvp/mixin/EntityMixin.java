package dev.cpvp.mixin;

import dev.cpvp.module.impl.Modules.HitboxesModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void cpvp$margin(CallbackInfoReturnable<Float> cir) {
        Object self = this;
        if (HitboxesModule.ACTIVE && self instanceof PlayerEntity && self != MinecraftClient.getInstance().player)
            cir.setReturnValue((float) HitboxesModule.expand());
    }
}
