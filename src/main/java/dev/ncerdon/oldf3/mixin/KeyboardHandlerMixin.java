package dev.ncerdon.oldf3.mixin;

import dev.ncerdon.oldf3.OldF3Keys;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
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

/** Brings back F3 + F, F3 + Q, and the 1.16.1 Shift+F3 (pie) / Alt+F3 (graphs) openers. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Shadow
    private void debugFeedbackComponent(Component component) {
        throw new AssertionError();
    }

    @Inject(method = "handleDebugKeys", at = @At("TAIL"), cancellable = true)
    private void oldf3$classicDebugKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        boolean handled = false;
        if (OldF3Keys.CYCLE_RENDER_DISTANCE.matches(event)) {
            OldF3Keys.cycleRenderDistance(minecraft, oldf3$shift(event), message -> debugFeedbackComponent(message));
            handled = true;
        }
        if (OldF3Keys.HELP.matches(event)) {
            OldF3Keys.showHelp(minecraft, message -> debugFeedbackComponent(message));
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
    private void oldf3$shiftAltCharts(long handle, int action, KeyEvent event, CallbackInfo ci) {
        boolean open = minecraft.debugEntries.isOverlayVisible();
        boolean graphs = open && (event.hasAltDown() || minecraft.hasAltDown());
        DebugScreenOverlayAccessor overlay = (DebugScreenOverlayAccessor) minecraft.getDebugOverlay();
        overlay.oldf3$setRenderProfilerChart(open && oldf3$shift(event));
        overlay.oldf3$setRenderFpsCharts(graphs);
        if (graphs) {
            // Same exclusivity vanilla applies when F3 + 2 turns the graphs on.
            overlay.oldf3$setRenderNetworkCharts(false);
            overlay.oldf3$setRenderLightmapTexture(false);
        }
    }

    /** Like 1.16.1's Screen.hasShiftDown(): trust the key itself, not only the event's modifier flags. */
    @Unique
    private boolean oldf3$shift(KeyEvent event) {
        return event.hasShiftDown() || minecraft.hasShiftDown();
    }
}
