package dev.cpvp.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow protected MinecraftClient client;

    /** Out of world: replace the panorama with a plain black gradient. */
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void cpvp$bg(DrawContext c, int mx, int my, float d, CallbackInfo ci) {
        if (client != null && client.world == null) {
            c.fillGradient(0, 0, c.getScaledWindowWidth(), c.getScaledWindowHeight(), 0xFF000000, 0xFF05070C);
            ci.cancel();
        }
    }
}
