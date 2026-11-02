package dev.ncerdon.speedrunport.mixin;

import dev.ncerdon.speedrunport.borderless.BorderlessToggle;
import dev.ncerdon.speedrunport.compat.Compat;
import dev.ncerdon.speedrunport.f3.ClassicDebugKeys;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Brings back F3 + F, F3 + Q, and the 1.16.1 Shift+F3 (pie) / Alt+F3 (graphs) openers, and
 * handles the borderless window key.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Shadow
    private void debugFeedbackComponent(Component component) {
        throw new AssertionError();
    }

    @Inject(method = "handleDebugKeys", at = @At("TAIL"), cancellable = true)
    private void speedrunport$classicDebugKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        boolean handled = false;
        if (ClassicDebugKeys.CYCLE_RENDER_DISTANCE.matches(event)) {
            ClassicDebugKeys.cycleRenderDistance(minecraft, speedrunport$shift(event), message -> debugFeedbackComponent(message));
            handled = true;
        }
        if (ClassicDebugKeys.HELP.matches(event)) {
            ClassicDebugKeys.showHelp(minecraft, message -> debugFeedbackComponent(message));
            handled = true;
        }
        if (handled) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 1.16.1: opening F3 with Shift held shows the pie, with Alt held shows the FPS/TPS graphs,
     * and a plain F3 shows neither.
     */
    @Inject(method = "keyPress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryList;toggleDebugOverlay()V",
            shift = At.Shift.AFTER))
    private void speedrunport$shiftAltCharts(long handle, int action, KeyEvent event, CallbackInfo ci) {
        boolean open = minecraft.debugEntries.isOverlayVisible();
        boolean graphs = open && (event.hasAltDown() || minecraft.hasAltDown());
        DebugScreenOverlayAccessor overlay = (DebugScreenOverlayAccessor) minecraft.getDebugOverlay();
        overlay.speedrunport$setRenderProfilerChart(open && speedrunport$shift(event));
        overlay.speedrunport$setRenderFpsCharts(graphs);
        if (graphs) {
            // Same exclusivity vanilla applies when F3 + 2 turns the graphs on.
            overlay.speedrunport$setRenderNetworkCharts(false);
            overlay.speedrunport$setRenderLightmapTexture(false);
        }
    }

    /**
     * The borderless key works like F11: in game and in menus, but not while F3 is held (F3 + F12
     * isn't a thing) and not in Controls, so it can still be rebound there.
     */
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void speedrunport$borderlessKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 1 && handle == Compat.windowHandle(minecraft.getWindow())
                && !(Compat.screen(minecraft) instanceof KeyBindsScreen)
                && !minecraft.options.keyDebugModifier.isDown()
                && BorderlessToggle.KEY.matches(event)) {
            BorderlessToggle.toggle(minecraft);
            ci.cancel();
        }
    }

    /** Like 1.16.1's Screen.hasShiftDown(): trust the key itself, not only the event's modifier flags. */
    @Unique
    private boolean speedrunport$shift(KeyEvent event) {
        return event.hasShiftDown() || minecraft.hasShiftDown();
    }
}
