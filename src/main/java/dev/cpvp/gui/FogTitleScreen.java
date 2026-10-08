package dev.cpvp.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.util.Random;

import static dev.cpvp.gui.RenderUtil.*;

/** Home screen: full-screen drifting smoke, centered logo, animated button stack. */
public final class FogTitleScreen extends Screen {
    private static final String[] LABELS = { "Singleplayer", "Multiplayer", "Options", "Quit Game" };
    private static final int[] ICONS = { 3, 5, 6, -1 };
    private static final int BW = 260, BH = 34, GAP = 8;

    private static final class Puff { float x, y, vx, vy, r, a, phase; }
    private final Puff[] puffs = new Puff[60];
    private final float[] hover = new float[LABELS.length];
    private final long start = System.nanoTime();
    private long last = System.nanoTime();
    private final Random rnd = new Random();

    public FogTitleScreen() {
        super(Text.literal("King Client"));
        for (int i = 0; i < puffs.length; i++) { puffs[i] = new Puff(); reset(puffs[i], true, 960, 540); }
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    private void reset(Puff p, boolean anywhere, int w, int h) {
        p.x = rnd.nextFloat() * Math.max(100, w);
        p.y = anywhere ? rnd.nextFloat() * h : h + 80;
        p.r = 50 + rnd.nextFloat() * 100;
        p.vx = (rnd.nextFloat() - 0.4f) * 12f;
        p.vy = -(6f + rnd.nextFloat() * 14f);
        p.a = 0.03f + rnd.nextFloat() * 0.04f;
        p.phase = rnd.nextFloat() * 6.28f;
    }

    private static void disc(DrawContext c, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy += 2) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            c.fill(cx - half, cy + dy, cx + half, cy + dy + 2, color);
        }
    }

    private static float ease(float p) { p = Math.max(0, Math.min(1, p)); return 1f - (1f - p) * (1f - p) * (1f - p); }

    private int[] rect(int i) { return new int[] { (width - BW) / 2, height / 2 - 14 + i * (BH + GAP), BW, BH }; }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        float t = (now - start) / 1e9f;

        c.fillGradient(0, 0, width, height, 0xFF000000, 0xFF040A07);

        for (Puff p : puffs) {
            p.x += (p.vx + (float) Math.sin(t * 0.4 + p.phase) * 6f) * dt;
            p.y += p.vy * dt;
            if (p.y < -p.r * 1.5f) reset(p, false, width, height);
            int cx = Math.round(p.x), cy = Math.round(p.y), r = Math.round(p.r);
            disc(c, cx, cy, r, alpha(0xFFB4D2C2, p.a));
            disc(c, cx, cy, Math.round(r * 0.7f), alpha(0xFFB4D2C2, p.a));
            disc(c, cx, cy, Math.round(r * 0.42f), alpha(ACCENT, p.a * 0.8f));
        }
        c.fillGradient(0, 0, width, height / 3, 0xB0000000, 0x00000000);
        c.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xC0000000);

        // logo drops in
        float la = ease((t - 0.1f) / 0.7f);
        float s = 2.2f;
        int lw = logoWidth(s);
        logo(c, (width - lw) / 2, height / 2 - 150 - Math.round((1 - la) * 40), s);

        for (int i = 0; i < LABELS.length; i++) {
            float p = ease((t - 0.45f - i * 0.09f) / 0.45f);
            int[] r = rect(i);
            int x = r[0], y = r[1] + Math.round((1 - p) * 24);
            boolean hov = p > 0.9f && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
            hover[i] += ((hov ? 1f : 0f) - hover[i]) * Math.min(1f, dt * 14f);
            float h = hover[i];

            roundRect(c, x, y, x + BW, y + BH, 4, alpha(0xFF101010, p * 0.92f));
            outline(c, x, y, x + BW, y + BH, alpha(0xFF2A2A2A, p));
            if (h > 0.01f) {
                c.fill(x + 1, y + 1, x + 1 + Math.round(h * (BW - 2)), y + BH - 1, alpha(ACCENT, 0.12f * p));
                c.fill(x + 1, y + BH - 2, x + 1 + Math.round(h * (BW - 2)), y + BH - 1, alpha(ACCENT, p));
            }
            int ic = alpha(hov ? ACCENT : ASH, p);
            if (ICONS[i] >= 0) icon(c, ICONS[i], x + 16, y + 10, ic); else cross(c, x + 19, y + 14, ic);
            text(c, LABELS[i], x + 48 + Math.round(h * 4), y + 11, alpha(hov ? 0xFFFFFFFF : TEXT, p), 1.0f);
        }
        text(c, "made by Pancakesupreme", 10, height - 16, alpha(DIM, la), 0.75f);
        String v = "Minecraft 1.21.1";
        text(c, v, width - 10 - width(v, 0.75f), height - 16, alpha(DIM, la), 0.75f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < LABELS.length; i++) {
            int[] r = rect(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                switch (i) {
                    case 0 -> client.setScreen(new SelectWorldScreen(this));
                    case 1 -> client.setScreen(new MultiplayerScreen(this));
                    case 2 -> client.setScreen(new OptionsScreen(this, client.options));
                    default -> client.scheduleStop();
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
