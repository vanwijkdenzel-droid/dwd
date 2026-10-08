package dev.cpvp.gui;

import dev.cpvp.module.Module;
import dev.cpvp.module.ModuleManager;
import dev.cpvp.module.impl.Modules.HudElement;
import dev.cpvp.module.impl.Modules.InterfaceModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import static dev.cpvp.gui.RenderUtil.*;

/**
 * Layout editor: drag your HUD mods (armor, CPS, keystrokes, logo...) AND the menu windows (navigator and every
 * open category window) to where you want them. ESC goes back.
 */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudElement drag;
    private MenuWindow dragWin;
    private int dx, dy;

    public HudEditorScreen(Screen parent) { super(Text.literal("Layout")); this.parent = parent; MenuWindow.init(); }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { dev.cpvp.ProfileManager.saveCurrent(); client.setScreen(parent); }

    private float scale() {
        InterfaceModule ui = ModuleManager.INSTANCE.get(InterfaceModule.class);
        return ui == null ? 0.85f : (float) ui.guiScale.get();
    }

    private boolean editable(MenuWindow w) { return w == MenuWindow.NAV || (w.cat != null && w.open); }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        ACCENT = ModuleManager.INSTANCE.get(InterfaceModule.class).accent();
        c.fill(0, 0, width, height, 0x70000000);
        float s = scale();
        int smx = Math.round(mx / s), smy = Math.round(my / s);

        // menu windows (drawn in the same scaled space the menu uses)
        var ms = c.getMatrices();
        ms.push();
        ms.scale(s, s, 1f);
        for (MenuWindow w : MenuWindow.ALL) {
            if (!editable(w)) continue;
            int h = Math.max(28, w.h);
            boolean hov = smx >= w.x && smx < w.x + w.w && smy >= w.y && smy < w.y + h;
            roundRect(c, w.x, w.y, w.x + w.w, w.y + h, 4, 0xD01A1A1A);
            roundRect(c, w.x, w.y, w.x + w.w, w.y + 28, 4, BLACK);
            text(c, w.title, w.x + 12, w.y + 9, TEXT, 0.95f);
            outline(c, w.x, w.y, w.x + w.w, w.y + h, hov || w == dragWin ? ACCENT : alpha(ACCENT, 0.4f));
        }
        ms.pop();

        // HUD mods
        boolean any = false;
        for (Module m : ModuleManager.INSTANCE.all()) {
            if (!(m instanceof HudElement h) || !m.isEnabled()) continue;
            any = true;
            h.draw(c, h.x(), h.y());
            boolean hov = mx >= h.x() && mx < h.x() + h.width() && my >= h.y() && my < h.y() + h.height();
            outline(c, h.x() - 1, h.y() - 1, h.x() + h.width() + 1, h.y() + h.height() + 1, hov || h == drag ? ACCENT : alpha(ACCENT, 0.4f));
            text(c, m.getName(), h.x(), h.y() - 10, ASH, 0.7f);
        }
        centerText(c, "Layout editor  -  drag HUD mods and menu windows, ESC to go back", width / 2, 12, TEXT, 0.95f);
        if (!any) centerText(c, "No HUD mods enabled. Turn some on in the Mods window first.", width / 2, height - 24, ASH, 0.85f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = ModuleManager.INSTANCE.all().size() - 1; i >= 0; i--) {
            Module m = ModuleManager.INSTANCE.all().get(i);
            if (!(m instanceof HudElement h) || !m.isEnabled()) continue;
            if (mx >= h.x() && mx < h.x() + h.width() && my >= h.y() && my < h.y() + h.height()) {
                drag = h; dx = (int) mx - h.x(); dy = (int) my - h.y();
                return true;
            }
        }
        float s = scale();
        double smx = mx / s, smy = my / s;
        for (int i = MenuWindow.ALL.size() - 1; i >= 0; i--) {
            MenuWindow w = MenuWindow.ALL.get(i);
            if (!editable(w)) continue;
            if (smx >= w.x && smx < w.x + w.w && smy >= w.y && smy < w.y + Math.max(28, w.h)) {
                dragWin = w; dx = (int) smx - w.x; dy = (int) smy - w.y;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (drag != null) {
            drag.setPos(MathHelper.clamp((int) mx - dx, 0, width - drag.width()), MathHelper.clamp((int) my - dy, 0, height - drag.height()));
            return true;
        }
        if (dragWin != null) {
            float s = scale();
            dragWin.x = MathHelper.clamp((int) (mx / s) - dx, -dragWin.w + 40, Math.round(width / s) - 40);
            dragWin.y = MathHelper.clamp((int) (my / s) - dy, 0, Math.round(height / s) - 28);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drag != null || dragWin != null) dev.cpvp.ProfileManager.markDirty();
        drag = null; dragWin = null;
        return true;
    }
}
