package dev.ncerdon.oldf3.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ncerdon.oldf3.OldF3Client;
import dev.ncerdon.oldf3.render.ClassicGraphs;
import dev.ncerdon.oldf3.render.LeftColumn;
import dev.ncerdon.oldf3.render.LineCollector;
import dev.ncerdon.oldf3.render.RightColumn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debugchart.FpsDebugChart;
import net.minecraft.client.gui.components.debugchart.TpsDebugChart;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * While the F3 overlay is open, swaps 26.3's per-entry text for the 1.16.1 columns and its charts
 * for the 1.16.1 graphs. When the overlay is closed (only "always on" entries showing), vanilla
 * runs untouched.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Font font;
    @Shadow @Final private LocalSampleLogger frameTimeLogger;
    @Shadow @Final private LocalSampleLogger tickTimeLogger;

    @Unique private final List<String> oldf3$otherModLines = new ArrayList<>();
    @Unique private boolean oldf3$loggedFailure;

    @Shadow
    private @Nullable Level getLevel() {
        throw new AssertionError();
    }

    @Shadow
    private @Nullable LevelChunk getClientChunk() {
        throw new AssertionError();
    }

    @Shadow
    private @Nullable LevelChunk getServerChunk() {
        throw new AssertionError();
    }

    @Shadow
    public abstract boolean showProfilerChart();

    @Shadow
    public abstract boolean showFpsCharts();

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void oldf3$resetOtherModLines(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        oldf3$otherModLines.clear();
    }

    /** 1.16.1 always drew the full screen, even if every 26.3 entry is switched off in F3 + F6. */
    @ModifyExpressionValue(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z", ordinal = 0))
    private boolean oldf3$drawWhileOverlayOpen(boolean noEntries) {
        return noEntries && !minecraft.debugEntries.isOverlayVisible();
    }

    /** Vanilla's entries are replaced by our columns; other mods' entries still run, into the right column. */
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntry;display(Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)V"))
    private void oldf3$replaceVanillaEntries(DebugScreenEntry entry, DebugScreenDisplayer displayer, @Nullable Level level,
                                             @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk,
                                             Operation<Void> original) {
        if (!minecraft.debugEntries.isOverlayVisible()) {
            original.call(entry, displayer, level, clientChunk, serverChunk);
        } else if (!entry.getClass().getName().startsWith("net.minecraft.")) {
            original.call(entry, new LineCollector(oldf3$otherModLines), level, clientChunk, serverChunk);
        }
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;ZI)V"))
    private void oldf3$classicColumns(DebugScreenOverlay overlay, GuiGraphicsExtractor graphics, List<String> lines,
                                      boolean alignLeft, int screenWidth, Operation<Void> original) {
        if (minecraft.debugEntries.isOverlayVisible()) {
            try {
                lines = alignLeft
                        ? LeftColumn.build(minecraft, getLevel(), getClientChunk(), getServerChunk(), showProfilerChart(), showFpsCharts())
                        : RightColumn.build(minecraft, oldf3$otherModLines);
            } catch (RuntimeException e) {
                // Fall back to vanilla's text for this frame rather than taking the HUD down.
                if (!oldf3$loggedFailure) {
                    oldf3$loggedFailure = true;
                    OldF3Client.LOGGER.error("Failed to build the classic F3 column", e);
                }
            }
        }
        original.call(overlay, graphics, lines, alignLeft, screenWidth);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debugchart/FpsDebugChart;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V"))
    private void oldf3$classicFrameGraph(FpsDebugChart chart, GuiGraphicsExtractor graphics, int left, int width, int bottom,
                                         Operation<Void> original) {
        // Vanilla passes min(242, screenWidth / 2); 1.16.1 used min(240, screenWidth / 2).
        ClassicGraphs.frameTimes(graphics, font, frameTimeLogger, left, Math.min(ClassicGraphs.MAX_WIDTH, width), bottom,
                minecraft.options.framerateLimit().get());
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debugchart/TpsDebugChart;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V"))
    private void oldf3$classicTickGraph(TpsDebugChart chart, GuiGraphicsExtractor graphics, int left, int width, int bottom,
                                        Operation<Void> original) {
        int screenWidth = left + width;
        int graphWidth = Math.min(ClassicGraphs.MAX_WIDTH, screenWidth / 2);
        float millisPerTick = minecraft.level != null ? minecraft.level.tickRateManager().millisecondsPerTick() : 50.0F;
        ClassicGraphs.tickTimes(graphics, font, tickTimeLogger, screenWidth - graphWidth, graphWidth, bottom, millisPerTick);
    }
}
