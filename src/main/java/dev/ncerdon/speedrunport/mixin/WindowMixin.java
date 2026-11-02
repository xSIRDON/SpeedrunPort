package dev.ncerdon.speedrunport.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.ncerdon.speedrunport.SpeedrunPortClient;
import dev.ncerdon.speedrunport.SpeedrunPortConfig;
import dev.ncerdon.speedrunport.borderless.BorderlessWindow;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Borderless window: an ordinary window with no decorations, exactly covering the monitor it is on.
 * Unlike 26.3's own non-exclusive fullscreen it is not padded 1px onto the next monitor, the game
 * still treats it as a window (so tools can resize it), and turning it off restores the old window.
 */
@Mixin(Window.class)
public abstract class WindowMixin implements BorderlessWindow {
    @Shadow @Final private long handle;
    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private boolean fullscreen;

    /** What the player asked for. */
    @Unique private boolean speedrunport$wanted;
    /** Whether the window is currently in our borderless state (it may be in fullscreen on top of it). */
    @Unique private boolean speedrunport$applied;
    @Unique private int speedrunport$restoreX;
    @Unique private int speedrunport$restoreY;
    @Unique private int speedrunport$restoreWidth;
    @Unique private int speedrunport$restoreHeight;
    @Unique private boolean speedrunport$restoreMaximized;

    @Shadow
    private boolean setWindowSizeAndPosition(int x, int y, int width, int height) {
        throw new AssertionError();
    }

    @Shadow
    private void syncWindow() {
        throw new AssertionError();
    }

    @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;ZLjava/lang/String;Lcom/mojang/blaze3d/platform/MonitorManager;Lcom/mojang/renderpearl/api/device/GpuBackend;I)V",
            at = @At("TAIL"))
    private void speedrunport$restoreBorderlessOnLaunch(CallbackInfo ci) {
        if (SpeedrunPortConfig.borderlessWindow()) {
            speedrunport$setBorderless(true);
        }
    }

    /** Coming back from F11 fullscreen, vanilla puts the border back; put the borderless state back on top. */
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
        if (fullscreen) {
            return; // Fullscreen owns the window; we catch up when it ends.
        }
        if (speedrunport$wanted) {
            speedrunport$enter();
        } else if (speedrunport$applied) {
            speedrunport$exit();
        }
    }

    @Unique
    private void speedrunport$enter() {
        int display = SDLVideo.SDL_GetDisplayForWindow(handle);
        if (display == 0) {
            display = SDLVideo.SDL_GetPrimaryDisplay();
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            if (!SDLVideo.SDL_GetDisplayBounds(display, bounds)) {
                SpeedrunPortClient.LOGGER.warn("Borderless window: could not read monitor bounds: {}", SDLError.SDL_GetError());
                return;
            }
            if (!speedrunport$applied) {
                // Vanilla keeps the windowed bounds up to date while not fullscreen; remember them for later.
                speedrunport$restoreX = windowedX;
                speedrunport$restoreY = windowedY;
                speedrunport$restoreWidth = windowedWidth;
                speedrunport$restoreHeight = windowedHeight;
                speedrunport$restoreMaximized = (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_MAXIMIZED) != 0;
            }
            if ((SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_MAXIMIZED) != 0) {
                SDLVideo.SDL_RestoreWindow(handle);
            }
            SDLVideo.SDL_SetWindowBordered(handle, false);
            setWindowSizeAndPosition(bounds.x(), bounds.y(), bounds.w(), bounds.h());
            syncWindow();
            speedrunport$applied = true;
        }
    }

    @Unique
    private void speedrunport$exit() {
        SDLVideo.SDL_SetWindowBordered(handle, true);
        setWindowSizeAndPosition(speedrunport$restoreX, speedrunport$restoreY, speedrunport$restoreWidth, speedrunport$restoreHeight);
        if (speedrunport$restoreMaximized) {
            SDLVideo.SDL_MaximizeWindow(handle);
        }
        syncWindow();
        speedrunport$applied = false;
    }
}
