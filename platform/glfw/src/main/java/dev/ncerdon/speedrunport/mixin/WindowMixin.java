package dev.ncerdon.speedrunport.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.ncerdon.speedrunport.SpeedrunPortConfig;
import dev.ncerdon.speedrunport.borderless.BorderlessBounds;
import dev.ncerdon.speedrunport.borderless.BorderlessBounds.Rect;
import dev.ncerdon.speedrunport.borderless.BorderlessWindow;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

@Mixin(Window.class)
public abstract class WindowMixin implements BorderlessWindow {
    @Shadow @Final private long handle;
    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;

    @Unique private boolean speedrunport$wanted;
    @Unique private boolean speedrunport$applied;
    @Unique private int speedrunport$restoreX;
    @Unique private int speedrunport$restoreY;
    @Unique private int speedrunport$restoreWidth;
    @Unique private int speedrunport$restoreHeight;
    @Unique private boolean speedrunport$restoreMaximized;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void speedrunport$restoreBorderlessOnLaunch(CallbackInfo ci) {
        if (SpeedrunPortConfig.borderlessWindow()) {
            speedrunport$setBorderless(true);
        }
    }

    @Inject(method = "setMode", at = @At("TAIL"))
    private void speedrunport$reapplyAfterModeChange(CallbackInfo ci) {
        speedrunport$sync();
    }

    @Override
    public boolean speedrunport$isBorderless() {
        return speedrunport$wanted;
    }

    @Override
    public void speedrunport$setBorderless(boolean borderless) {
        speedrunport$wanted = borderless;
        speedrunport$sync();
    }

    @Unique
    private void speedrunport$sync() {
        if (GLFW.glfwGetWindowMonitor(handle) != 0L) {
            return;
        }
        if (speedrunport$wanted) {
            speedrunport$enter();
        } else if (speedrunport$applied) {
            speedrunport$exit();
        }
    }

    @Unique
    private void speedrunport$enter() {
        List<Rect> monitors = speedrunport$monitors();
        if (monitors.isEmpty()) {
            return;
        }
        Rect monitor = speedrunport$monitorOfWindow(monitors);
        List<Rect> others = new ArrayList<>(monitors);
        others.remove(monitor);

        boolean maximized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
        if (!speedrunport$applied) {
            speedrunport$restoreX = windowedX;
            speedrunport$restoreY = windowedY;
            speedrunport$restoreWidth = windowedWidth;
            speedrunport$restoreHeight = windowedHeight;
            speedrunport$restoreMaximized = maximized;
        }
        if (maximized) {
            GLFW.glfwRestoreWindow(handle);
        }
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        Rect target = BorderlessBounds.padded(monitor, others);
        GLFW.glfwSetWindowMonitor(handle, 0L, target.x(), target.y(), target.width(), target.height(), GLFW.GLFW_DONT_CARE);
        speedrunport$applied = true;
    }

    @Unique
    private void speedrunport$exit() {
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        GLFW.glfwSetWindowMonitor(handle, 0L, speedrunport$restoreX, speedrunport$restoreY,
                speedrunport$restoreWidth, speedrunport$restoreHeight, GLFW.GLFW_DONT_CARE);
        if (speedrunport$restoreMaximized) {
            GLFW.glfwMaximizeWindow(handle);
        }
        speedrunport$applied = false;
    }

    @Unique
    private Rect speedrunport$monitorOfWindow(List<Rect> monitors) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(handle, x, y);
            GLFW.glfwGetWindowSize(handle, w, h);
            int cx = x.get(0) + w.get(0) / 2;
            int cy = y.get(0) + h.get(0) / 2;
            for (Rect monitor : monitors) {
                if (cx >= monitor.x() && cx < monitor.x() + monitor.width() && cy >= monitor.y() && cy < monitor.y() + monitor.height()) {
                    return monitor;
                }
            }
        }
        return monitors.getFirst();
    }

    @Unique
    private static List<Rect> speedrunport$monitors() {
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
