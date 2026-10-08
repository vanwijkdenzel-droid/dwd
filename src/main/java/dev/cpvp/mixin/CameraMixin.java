package dev.cpvp.mixin;

import dev.cpvp.util.CameraControl;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void cpvp$update(BlockView area, Entity focused, boolean tp, boolean inv, float td, CallbackInfo ci) {
        if (!CameraControl.active) return;
        setPos(CameraControl.x, CameraControl.y, CameraControl.z);
        setRotation(CameraControl.yaw, CameraControl.pitch);
        thirdPerson = true; // draws your player model like spectator view
    }
}
