package dev.cpvp.mixin;

import dev.cpvp.module.ModuleManager;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    /**
     * Fires on the raw GLFW press, before KeyBinding state is updated and before the
     * next tick's handleInputEvents - this is what makes the click path "instant".
     */
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void cpvp$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) ModuleManager.INSTANCE.onMouseClick(button);
    }
}
