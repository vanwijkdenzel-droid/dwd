package dev.cpvp.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/** 2D helpers: rounded rects, TTF text, pixel icons, the King Client logo. */
public final class RenderUtil {
    /** Theme colour - updated every frame from the Interface module. */
    public static int ACCENT = 0xFF30F271;

    public static final int BLACK   = 0xFF000000;
    public static final int CROWN   = 0xFFFFD23F;
    public static final int NAV     = 0xFF1A1A1A;   // light-gray panels like Vape
    public static final int PANEL   = 0xFF1A1A1A;
    public static final int HEADER  = 0xFF141414;
    public static final int STRIP   = 0xFF0C0C0C;   // darker strips (MISC, footer)
    public static final int ROW_HOV = 0xFF232323;
    public static final int ROW_SEL = 0xFF222222;
    public static final int INNER   = 0xFF121212;
    public static final int TEXT    = 0xFFE8E8E8;
    public static final int ASH     = 0xFFA0A0A0;
    public static final int DIM     = 0xFF6E6E6E;
    public static final int TRACK   = 0xFF2B2B2B;
    public static final int BORDER  = 0xFF000000;

    private static final Style FONT = Style.EMPTY.withFont(Identifier.of("cpvpclient", "ui"));

    private RenderUtil() {}

    public static int alpha(int argb, float mul) {
        int a = Math.round(((argb >>> 24) & 0xFF) * mul);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static void roundRect(DrawContext c, int x, int y, int x2, int y2, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min((x2 - x) / 2, (y2 - y) / 2)));
        if (r == 0) { c.fill(x, y, x2, y2, color); return; }
        c.fill(x, y + r, x2, y2 - r, color);
        int soft = alpha(color, 0.5f);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.floor(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset + 1, y + i, x2 - inset - 1, y + i + 1, color);
            c.fill(x + inset + 1, y2 - i - 1, x2 - inset - 1, y2 - i, color);
            c.fill(x + inset, y + i, x + inset + 1, y + i + 1, soft);
            c.fill(x2 - inset - 1, y + i, x2 - inset, y + i + 1, soft);
            c.fill(x + inset, y2 - i - 1, x + inset + 1, y2 - i, soft);
            c.fill(x2 - inset - 1, y2 - i - 1, x2 - inset, y2 - i, soft);
        }
    }

    public static void outline(DrawContext c, int x, int y, int x2, int y2, int color) {
        c.fill(x, y, x2, y + 1, color); c.fill(x, y2 - 1, x2, y2, color);
        c.fill(x, y, x + 1, y2, color); c.fill(x2 - 1, y, x2, y2, color);
    }

    // ---- text ----
    private static Text styled(String s, boolean bold) { return Text.literal(s).setStyle(bold ? FONT.withBold(true) : FONT); }

    public static void text(DrawContext c, String s, int x, int y, int color, float scale) {
        var m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        c.drawText(MinecraftClient.getInstance().textRenderer, styled(s, false), 0, 0, color, false);
        m.pop();
    }

    public static int width(String s, float scale) {
        return Math.round(MinecraftClient.getInstance().textRenderer.getWidth(styled(s, false)) * scale);
    }

    public static void centerText(DrawContext c, String s, int cx, int y, int color, float scale) {
        text(c, s, cx - width(s, scale) / 2, y, color, scale);
    }

    private static void italicBold(DrawContext c, String s, float x, float y, float scale, int color) {
        var m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.multiplyPositionMatrix(new Matrix4f().m10(-0.22f));
        m.scale(scale, scale, 1f);
        c.drawText(MinecraftClient.getInstance().textRenderer, styled(s, true), 0, 0, color, false);
        m.pop();
    }

    // ---- icons: 7x7 bitmaps, drawn at 2x ----
    private static final String[][] ICONS = {
        { "....###", "...###.", "#.###..", ".###...", "..##...", ".#.#...", "#......" }, // 0 combat
        { ".......", "..###..", ".#####.", "##.#.##", ".#####.", "..###..", "......." }, // 1 render
        { ".##....", "####...", ".###...", "..###..", "...###.", "....###", ".....##" }, // 2 utility
        { "..###..", ".#.#.#.", "#.###.#", "#######", "#.###.#", ".#.#.#.", "..###.." }, // 3 world
        { ".#####.", "#######", "#.....#", "#######", "#..#..#", "#.....#", "#######" }, // 4 inventory
        { "......#", "....#.#", "..#.#.#", "#.#.#.#", "#.#.#.#", "#.#.#.#", "#.#.#.#" }, // 5 network
        { "..#.#..", ".#####.", "###.###", ".#...#.", "###.###", ".#####.", "..#.#.." }, // 6 settings
        { "###.###", "###.###", "###.###", ".......", "###.###", "###.###", "###.###" }, // 7 mods
        { "#######", ".......", "#.#####", ".......", "#.#####", ".......", "#.#####" }  // 8 profiles
    };
    public static final int ICON_PROFILES = 8;

    public static void icon(DrawContext c, int id, int x, int y, int color) { bitmap(c, ICONS[id], x, y, 2, color); }

    private static void bitmap(DrawContext c, String[] rows, int x, int y, int px, int color) {
        for (int j = 0; j < rows.length; j++)
            for (int i = 0; i < rows[j].length(); i++)
                if (rows[j].charAt(i) == '#') c.fill(x + i * px, y + j * px, x + (i + 1) * px, y + (j + 1) * px, color);
    }

    private static final String[] CHEVRON_R = { "#..", ".#.", "..#", ".#.", "#.." };
    private static final String[] CHEVRON_D = { "#...#", ".#.#.", "..#.." };
    private static final String[] CHEVRON_U = { "..#..", ".#.#.", "#...#" };
    private static final String[] CHECK = {
        "........", "......#.", ".....##.", "#...##..", "##.##...", ".###....", "..#.....", "........" };
    private static final String[] CROSS = { "#...#", ".#.#.", "..#..", ".#.#.", "#...#" };

    public static void chevronRight(DrawContext c, int x, int y, int color) { bitmap(c, CHEVRON_R, x, y, 1, color); }
    public static void chevronDown(DrawContext c, int x, int y, int color)  { bitmap(c, CHEVRON_D, x, y, 1, color); }
    public static void chevronUp(DrawContext c, int x, int y, int color)    { bitmap(c, CHEVRON_U, x, y, 1, color); }
    public static void cross(DrawContext c, int x, int y, int color)        { bitmap(c, CROSS, x, y, 1, color); }

    public static void dots(DrawContext c, int x, int y, int color) {
        for (int i = 0; i < 3; i++) c.fill(x, y + i * 4, x + 2, y + i * 4 + 2, color);
    }

    public static void checkbox(DrawContext c, int x, int y, int size, boolean checked) {
        roundRect(c, x, y, x + size, y + size, 1, checked ? ACCENT : TRACK);
        if (checked) bitmap(c, CHECK, x + (size - 8) / 2, y + (size - 8) / 2, 1, 0xFF050505);
    }

    // ---- logo ----
    public static void line(DrawContext c, float x1, float y1, float x2, float y2, int th, int color) {
        int steps = (int) Math.max(1, Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)));
        for (int i = 0; i <= steps; i++) {
            int px = Math.round(x1 + (x2 - x1) * i / steps), py = Math.round(y1 + (y2 - y1) * i / steps);
            c.fill(px, py, px + th, py + th, color);
        }
    }

    private static void sparkle(DrawContext c, int cx, int cy, int r, int color) {
        c.fill(cx - 1, cy - r, cx + 1, cy + r, color);
        c.fill(cx - r, cy - 1, cx + r, cy + 1, color);
        c.fill(cx - 2, cy - 2, cx + 2, cy + 2, color);
    }

    private static final float TS = 2.1f, BS = 1.35f;

    public static int logoWidth(float s) {
        return Math.round(width("KING", TS * s)) + Math.round(width("CLIENT", BS * s)) + Math.round(24 * s);
    }

    public static int logoHeight(float s) { return Math.round(34 * s); }

    /** KING (italic white) + CLIENT in a slanted green frame, crown above, sparkles around. */
    public static void logo(DrawContext c, int x, int y, float s) {
        int th = Math.max(1, Math.round(1.4f * s));
        float ty = y + 12 * s, h = 19 * s;
        int fogW = Math.round(width("KING", TS * s)) + Math.round(3 * s);
        italicBold(c, "KING", x + 3 * s, ty, TS * s, 0xFFFFFFFF);
        // V1 frame
        float bx = x + fogW + 6 * s, bw = width("CLIENT", BS * s) + 12 * s, sk = 5 * s;
        line(c, bx + sk, ty - 2 * s, bx + bw + sk - 3 * s, ty - 2 * s, th, ACCENT);
        line(c, bx + bw + sk - 3 * s, ty - 2 * s, bx + bw, ty + h, th, ACCENT);
        line(c, bx + bw, ty + h, bx, ty + h, th, ACCENT);
        line(c, bx + sk, ty - 2 * s, bx, ty + h, th, ACCENT);
        italicBold(c, "CLIENT", bx + 6 * s, ty + 5 * s, BS * s, ACCENT);
        // crown
        float cx0 = x + fogW - 6 * s, cy0 = y;
        float u = 1.3f * s;
        float[][] pts = { {0, 9}, {0, 3}, {3, 6}, {5, 0}, {7, 6}, {10, 3}, {10, 9}, {0, 9} };
        for (int i = 0; i < pts.length - 1; i++)
            line(c, cx0 + pts[i][0] * u, cy0 + pts[i][1] * u, cx0 + pts[i + 1][0] * u, cy0 + pts[i + 1][1] * u, th, ACCENT);
        // sparkles
        sparkle(c, x - Math.round(2 * s), Math.round(ty + 10 * s), Math.max(3, Math.round(4 * s)), ACCENT);
        sparkle(c, x + Math.round(fogW * 0.45f), Math.round(ty + h + 6 * s), Math.max(3, Math.round(3.5f * s)), ACCENT);
        sparkle(c, Math.round(bx + bw + sk + 4 * s), Math.round(ty - 6 * s), Math.max(3, Math.round(3.5f * s)), ACCENT);
    }

    /** Solid yellow crown (no text). */
    public static void crown(DrawContext c, int x, int y, int size, int color) {
        float u = size / 10f;
        int th = Math.max(2, Math.round(u * 1.3f));
        float[][] p = { {0, 9}, {0, 3}, {3, 6}, {5, 0}, {7, 6}, {10, 3}, {10, 9}, {0, 9} };
        for (int i = 0; i < p.length - 1; i++)
            line(c, x + p[i][0] * u, y + p[i][1] * u, x + p[i + 1][0] * u, y + p[i + 1][1] * u, th, color);
        // fill the body between the base line and the zig-zag
        for (int px = 0; px <= Math.round(10 * u); px++) {
            float t = px / u;
            float top = t <= 3 ? 3 + t : t <= 5 ? 6 - (t - 3) * 3 : t <= 7 ? (t - 5) * 3 : 6 - (t - 7);
            if (t <= 3) top = 3 + t; else if (t <= 5) top = 6 - (t - 3) * 3; else if (t <= 7) top = (t - 5) * 3; else top = 6 - (t - 7) * 1;
            if (t <= 3) top = 3 + t * 1f;
            c.fill(x + px, y + Math.round(top * u), x + px + 1, y + Math.round(9.5f * u), alpha(color, 0.55f));
        }
        c.fill(x, y + Math.round(9.5f * u), x + Math.round(10 * u) + th, y + Math.round(9.5f * u) + th, color);
    }
}
