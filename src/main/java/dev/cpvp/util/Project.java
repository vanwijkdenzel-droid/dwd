package dev.cpvp.util;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Captures the world matrices each frame so HUD modules can place 2D elements over 3D positions. */
public final class Project {
    private static final Matrix4f PROJ = new Matrix4f(), MV = new Matrix4f();
    private static Vec3d cam = Vec3d.ZERO;
    private static boolean valid;
    private Project() {}

    public static void capture(WorldRenderContext ctx) {
        PROJ.set(ctx.projectionMatrix());
        MV.set(ctx.matrixStack().peek().getPositionMatrix());
        cam = ctx.camera().getPos();
        valid = true;
    }

    /** Scaled-GUI coordinates {x, y}, or null when the point is behind the camera. */
    public static float[] toScreen(Vec3d p) {
        if (!valid) return null;
        Vector4f v = new Vector4f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z), 1f);
        MV.transform(v);
        PROJ.transform(v);
        if (v.w <= 0.05f) return null;
        var w = MinecraftClient.getInstance().getWindow();
        return new float[] { (v.x / v.w * 0.5f + 0.5f) * w.getScaledWidth(), (1f - (v.y / v.w * 0.5f + 0.5f)) * w.getScaledHeight() };
    }
}
