package dev.cpvp.mixin;

import dev.cpvp.gui.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flat black buttons everywhere (replaces the stone-textured vanilla look). */
@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {
    protected PressableWidgetMixin(int x, int y, int w, int h, Text message) { super(x, y, w, h, message); }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void cpvp$render(DrawContext c, int mx, int my, float d, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof ButtonWidget || self instanceof CyclingButtonWidget<?>) || self instanceof TexturedButtonWidget) return;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hov = isHovered() && active;
        c.fill(x, y, x + w, y + h, hov ? 0xFF101010 : 0xFF000000);
        c.fill(x, y, x + w, y + 1, 0xFF1C1C1C);
        c.fill(x, y + h - 1, x + w, y + h, hov ? RenderUtil.ACCENT : 0xFF1C1C1C);
        c.fill(x, y, x + 1, y + h, 0xFF1C1C1C);
        c.fill(x + w - 1, y, x + w, y + h, 0xFF1C1C1C);
        RenderUtil.centerText(c, getMessage().getString(), x + w / 2, y + (h - 9) / 2,
                active ? (hov ? 0xFFFFFFFF : 0xFFC8C8C8) : 0xFF555555, 1f);
        ci.cancel();
    }
}
