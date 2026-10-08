package dev.cpvp.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Through-wall box drawing (translucent fill + outline). Call begin() / box()... / end(). */
public final class Render3D {
    private Render3D() {}

    public static void begin() {
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2.5f);
    }

    public static void end() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void box(MatrixStack ms, Vec3d cam, Box b, int argb) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float x1 = (float) (b.minX - cam.x), y1 = (float) (b.minY - cam.y), z1 = (float) (b.minZ - cam.z);
        float x2 = (float) (b.maxX - cam.x), y2 = (float) (b.maxY - cam.y), z2 = (float) (b.maxZ - cam.z);
        float a = (argb >>> 24 & 255) / 255f, r = (argb >>> 16 & 255) / 255f,
              g = (argb >>> 8 & 255) / 255f,  bl = (argb & 255) / 255f;

        float fa = a * 0.16f;
        BufferBuilder q = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        face(q, m, x1,y1,z1, x2,y1,z1, x2,y1,z2, x1,y1,z2, r,g,bl,fa); // bottom
        face(q, m, x1,y2,z1, x1,y2,z2, x2,y2,z2, x2,y2,z1, r,g,bl,fa); // top
        face(q, m, x1,y1,z1, x1,y2,z1, x2,y2,z1, x2,y1,z1, r,g,bl,fa); // north
        face(q, m, x1,y1,z2, x2,y1,z2, x2,y2,z2, x1,y2,z2, r,g,bl,fa); // south
        face(q, m, x1,y1,z1, x1,y1,z2, x1,y2,z2, x1,y2,z1, r,g,bl,fa); // west
        face(q, m, x2,y1,z1, x2,y2,z1, x2,y2,z2, x2,y1,z2, r,g,bl,fa); // east
        BufferRenderer.drawWithGlobalProgram(q.end());

        BufferBuilder l = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        edge(l,m,x1,y1,z1,x2,y1,z1,r,g,bl,a); edge(l,m,x2,y1,z1,x2,y1,z2,r,g,bl,a);
        edge(l,m,x2,y1,z2,x1,y1,z2,r,g,bl,a); edge(l,m,x1,y1,z2,x1,y1,z1,r,g,bl,a);
        edge(l,m,x1,y2,z1,x2,y2,z1,r,g,bl,a); edge(l,m,x2,y2,z1,x2,y2,z2,r,g,bl,a);
        edge(l,m,x2,y2,z2,x1,y2,z2,r,g,bl,a); edge(l,m,x1,y2,z2,x1,y2,z1,r,g,bl,a);
        edge(l,m,x1,y1,z1,x1,y2,z1,r,g,bl,a); edge(l,m,x2,y1,z1,x2,y2,z1,r,g,bl,a);
        edge(l,m,x2,y1,z2,x2,y2,z2,r,g,bl,a); edge(l,m,x1,y1,z2,x1,y2,z2,r,g,bl,a);
        BufferRenderer.drawWithGlobalProgram(l.end());
    }

    private static void face(BufferBuilder b, Matrix4f m,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float bl, float a) {
        b.vertex(m, ax, ay, az).color(r, g, bl, a);
        b.vertex(m, bx, by, bz).color(r, g, bl, a);
        b.vertex(m, cx, cy, cz).color(r, g, bl, a);
        b.vertex(m, dx, dy, dz).color(r, g, bl, a);
    }

    private static void edge(BufferBuilder b, Matrix4f m, float x1, float y1, float z1,
                             float x2, float y2, float z2, float r, float g, float bl, float a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
    }

    public static void line(MatrixStack ms, Vec3d cam, Vec3d a, Vec3d b, int argb) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float al = (argb >>> 24 & 255) / 255f, r = (argb >>> 16 & 255) / 255f, g = (argb >>> 8 & 255) / 255f, bl = (argb & 255) / 255f;
        BufferBuilder l = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        l.vertex(m, (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z)).color(r, g, bl, al);
        l.vertex(m, (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z)).color(r, g, bl, al);
        BufferRenderer.drawWithGlobalProgram(l.end());
    }

    /** One batched buffer of box outlines (cheap for hundreds of boxes). */
    public static void outlines(MatrixStack ms, Vec3d cam, java.util.List<Box> boxes, int argb) {
        if (boxes.isEmpty()) return;
        Matrix4f m = ms.peek().getPositionMatrix();
        float a = (argb >>> 24 & 255) / 255f, r = (argb >>> 16 & 255) / 255f, g = (argb >>> 8 & 255) / 255f, bl = (argb & 255) / 255f;
        BufferBuilder l = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (Box b : boxes) {
            float x1 = (float) (b.minX - cam.x), y1 = (float) (b.minY - cam.y), z1 = (float) (b.minZ - cam.z);
            float x2 = (float) (b.maxX - cam.x), y2 = (float) (b.maxY - cam.y), z2 = (float) (b.maxZ - cam.z);
            edge(l,m,x1,y1,z1,x2,y1,z1,r,g,bl,a); edge(l,m,x2,y1,z1,x2,y1,z2,r,g,bl,a);
            edge(l,m,x2,y1,z2,x1,y1,z2,r,g,bl,a); edge(l,m,x1,y1,z2,x1,y1,z1,r,g,bl,a);
            edge(l,m,x1,y2,z1,x2,y2,z1,r,g,bl,a); edge(l,m,x2,y2,z1,x2,y2,z2,r,g,bl,a);
            edge(l,m,x2,y2,z2,x1,y2,z2,r,g,bl,a); edge(l,m,x1,y2,z2,x1,y2,z1,r,g,bl,a);
            edge(l,m,x1,y1,z1,x1,y2,z1,r,g,bl,a); edge(l,m,x2,y1,z1,x2,y2,z1,r,g,bl,a);
            edge(l,m,x2,y1,z2,x2,y2,z2,r,g,bl,a); edge(l,m,x1,y1,z2,x1,y2,z2,r,g,bl,a);
        }
        BufferRenderer.drawWithGlobalProgram(l.end());
    }

    /** Bright corner brackets - draw on top of box() for a cleaner look. */
    public static void corners(MatrixStack ms, Vec3d cam, Box b, int argb) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float a = (argb >>> 24 & 255) / 255f, r = (argb >>> 16 & 255) / 255f, g = (argb >>> 8 & 255) / 255f, bl = (argb & 255) / 255f;
        float x1 = (float) (b.minX - cam.x), y1 = (float) (b.minY - cam.y), z1 = (float) (b.minZ - cam.z);
        float x2 = (float) (b.maxX - cam.x), y2 = (float) (b.maxY - cam.y), z2 = (float) (b.maxZ - cam.z);
        float dx = (x2 - x1) * 0.3f, dz = (z2 - z1) * 0.3f;
        BufferBuilder l = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (float yy : new float[] { y1, y2 }) {
            for (float zz : new float[] { z1, z2 }) {
                edge(l, m, x1, yy, zz, x1 + dx, yy, zz, r, g, bl, a);
                edge(l, m, x2, yy, zz, x2 - dx, yy, zz, r, g, bl, a);
            }
            for (float xx : new float[] { x1, x2 }) {
                edge(l, m, xx, yy, z1, xx, yy, z1 + dz, r, g, bl, a);
                edge(l, m, xx, yy, z2, xx, yy, z2 - dz, r, g, bl, a);
            }
        }
        for (float xx : new float[] { x1, x2 }) for (float zz : new float[] { z1, z2 })
            edge(l, m, xx, y1, zz, xx, y2, zz, r, g, bl, a);
        BufferRenderer.drawWithGlobalProgram(l.end());
    }
}
