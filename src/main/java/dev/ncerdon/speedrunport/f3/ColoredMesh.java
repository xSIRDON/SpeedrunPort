package dev.ncerdon.speedrunport.f3;

import dev.ncerdon.speedrunport.compat.Compat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;

import java.util.Arrays;

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

    public void quad(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int color) {
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
        graphics.guiRenderState.addGuiElement(Compat.coloredQuads(pose,
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

    private static float cross(float ax, float ay, float bx, float by, float cx, float cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }
}
