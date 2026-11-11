package dev.ncerdon.speedrunport.gametest;

import dev.ncerdon.speedrunport.borderless.BorderlessBounds.Rect;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

// asks GLFW about the real window since the test harness fakes resize events
final class WindowProbe {
    private WindowProbe() {}

    static Rect windowRect(Minecraft mc) {
        long handle = mc.getWindow().handle();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(handle, x, y);
            GLFW.glfwGetWindowSize(handle, w, h);
            return new Rect(x.get(0), y.get(0), w.get(0), h.get(0));
        }
    }

    // monitor under the window's center, same as the mod
    static Rect monitorRect(Minecraft mc) {
        Rect window = windowRect(mc);
        int cx = window.x() + window.width() / 2;
        int cy = window.y() + window.height() / 2;
        List<Rect> monitors = monitors();
        for (Rect monitor : monitors) {
            if (cx >= monitor.x() && cx < monitor.x() + monitor.width() && cy >= monitor.y() && cy < monitor.y() + monitor.height()) {
                return monitor;
            }
        }
        return monitors.getFirst();
    }

    static List<Rect> otherMonitorRects(Minecraft mc) {
        Rect current = monitorRect(mc);
        List<Rect> others = new ArrayList<>(monitors());
        others.remove(current);
        return others;
    }

    static boolean hasBorder(Minecraft mc) {
        return GLFW.glfwGetWindowAttrib(mc.getWindow().handle(), GLFW.GLFW_DECORATED) == GLFW.GLFW_TRUE;
    }

    private static List<Rect> monitors() {
        List<Rect> monitors = new ArrayList<>();
        PointerBuffer handles = GLFW.glfwGetMonitors();
        if (handles == null) {
            return monitors;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            for (int i = 0; i < handles.limit(); i++) {
                long monitor = handles.get(i);
                GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
                if (mode != null) {
                    GLFW.glfwGetMonitorPos(monitor, x, y);
                    monitors.add(new Rect(x.get(0), y.get(0), mode.width(), mode.height()));
                }
            }
        }
        return monitors;
    }
}
