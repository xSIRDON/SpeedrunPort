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

// F3 + F, F3 + Q, shift/alt + F3, and the borderless key
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

    // like 1.16: shift + F3 opens it with the pie, alt + F3 with the graphs, plain F3 with neither
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
            // vanilla does the same when F3 + 2 turns the graphs on
            overlay.speedrunport$setRenderNetworkCharts(false);
            overlay.speedrunport$setRenderLightmapTexture(false);
        }
    }

    // works anywhere like F11, except with F3 held or in Controls (so it can still be rebound)
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

    // check the actual key too, not just the event's modifier flags (1.16 did hasShiftDown())
    @Unique
    private boolean speedrunport$shift(KeyEvent event) {
        return event.hasShiftDown() || minecraft.hasShiftDown();
    }
}
