package dev.ncerdon.speedrunport.gametest;

import dev.ncerdon.speedrunport.borderless.BorderlessBounds.Rect;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLStdinc;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

// asks SDL about the real window since the test harness fakes resize events
final class WindowProbe {
    private WindowProbe() {}

    static Rect windowRect(Minecraft mc) {
        long handle = mc.getWindow().handle();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowPosition(handle, x, y);
            SDLVideo.SDL_GetWindowSize(handle, w, h);
            return new Rect(x.get(0), y.get(0), w.get(0), h.get(0));
        }
    }

    static Rect monitorRect(Minecraft mc) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            SDLVideo.SDL_GetDisplayBounds(SDLVideo.SDL_GetDisplayForWindow(mc.getWindow().handle()), bounds);
            return new Rect(bounds.x(), bounds.y(), bounds.w(), bounds.h());
        }
    }

    static List<Rect> otherMonitorRects(Minecraft mc) {
        int current = SDLVideo.SDL_GetDisplayForWindow(mc.getWindow().handle());
        List<Rect> others = new ArrayList<>();
        IntBuffer displays = SDLVideo.SDL_GetDisplays();
        if (displays == null) {
            return others;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            for (int i = 0; i < displays.remaining(); i++) {
                if (displays.get(i) != current && SDLVideo.SDL_GetDisplayBounds(displays.get(i), bounds)) {
                    others.add(new Rect(bounds.x(), bounds.y(), bounds.w(), bounds.h()));
                }
            }
        } finally {
            SDLStdinc.SDL_free(displays);
        }
        return others;
    }

    static boolean hasBorder(Minecraft mc) {
        return (SDLVideo.SDL_GetWindowFlags(mc.getWindow().handle()) & SDLVideo.SDL_WINDOW_BORDERLESS) == 0;
    }
}
