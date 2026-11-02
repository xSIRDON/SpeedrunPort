package dev.ncerdon.oldf3.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Untextured colored triangles and quads for the GUI layer. 26.3's GUI only draws quads, so a
 * triangle is sent as a quad with its last corner repeated.
 */
public final class ColoredMesh {
    private float[] positions = new float[256];
    private int[] colors = new int[128];
    private int vertexCount;
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;

    public void triangle(float ax, float ay, float bx, float by, float cx, float cy, int color) {
        quad(ax, ay, bx, by, cx, cy, cx, cy, color);
    }

    /** Corners in order around the edge; either direction works. */
    public void quad(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int color) {
        // Match the corner order vanilla uses for filled rectangles so nothing is culled as a back face.
        if (cross(ax, ay, bx, by, cx, cy) > 0.0F || cross(ax, ay, cx, cy, dx, dy) > 0.0F) {
            vertex(ax, ay, color);
            vertex(dx, dy, color);
            vertex(cx, cy, color);
            vertex(bx, by, color);
        } else {
            vertex(ax, ay, color);
            vertex(bx, by, color);
            vertex(cx, cy, color);
            vertex(dx, dy, color);
        }
    }

    public void submit(GuiGraphicsExtractor graphics) {
        if (vertexCount == 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        int left = (int) Math.floor(minX);
        int top = (int) Math.floor(minY);
        ScreenRectangle bounds = new ScreenRectangle(left, top,
                (int) Math.ceil(maxX) - left, (int) Math.ceil(maxY) - top).transformMaxBounds(pose);
        graphics.guiRenderState.addGuiElement(new State(pose,
                Arrays.copyOf(positions, vertexCount * 2), Arrays.copyOf(colors, vertexCount),
                scissor, scissor != null ? scissor.intersection(bounds) : bounds));
    }

    private void vertex(float x, float y, int color) {
        if (vertexCount == colors.length) {
            positions = Arrays.copyOf(positions, positions.length * 2);
            colors = Arrays.copyOf(colors, colors.length * 2);
        }
        positions[vertexCount * 2] = x;
        positions[vertexCount * 2 + 1] = y;
        colors[vertexCount] = color;
        vertexCount++;
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
    }

    /** z of (b - a) x (c - a); negative is the winding vanilla's GUI rectangles use. */
    private static float cross(float ax, float ay, float bx, float by, float cx, float cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private record State(Matrix3x2fc pose, float[] positions, int[] colors,
                         @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (int i = 0; i < colors.length; i++) {
                consumer.addVertexWith2DPose(pose, positions[i * 2], positions[i * 2 + 1]).setColor(colors[i]);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}
