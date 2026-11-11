package dev.ncerdon.speedrunport.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ncerdon.speedrunport.SpeedrunPortClient;
import dev.ncerdon.speedrunport.f3.ClassicGraphs;
import dev.ncerdon.speedrunport.f3.LeftColumn;
import dev.ncerdon.speedrunport.f3.LineCollector;
import dev.ncerdon.speedrunport.f3.RightColumn;
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

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Font font;
    @Shadow @Final private LocalSampleLogger frameTimeLogger;
    @Shadow @Final private LocalSampleLogger tickTimeLogger;

    @Unique private final List<String> speedrunport$otherModLines = new ArrayList<>();
    @Unique private boolean speedrunport$loggedFailure;

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
    private void speedrunport$resetOtherModLines(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        speedrunport$otherModLines.clear();
    }

    @ModifyExpressionValue(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z", ordinal = 0))
    private boolean speedrunport$drawWhileOverlayOpen(boolean noEntries) {
        return noEntries && !minecraft.debugEntries.isOverlayVisible();
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntry;display(Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)V"))
    private void speedrunport$replaceVanillaEntries(DebugScreenEntry entry, DebugScreenDisplayer displayer, @Nullable Level level,
                                             @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk,
                                             Operation<Void> original) {
        if (!minecraft.debugEntries.isOverlayVisible()) {
            original.call(entry, displayer, level, clientChunk, serverChunk);
        } else if (!entry.getClass().getName().startsWith("net.minecraft.")) {
            original.call(entry, new LineCollector(speedrunport$otherModLines), level, clientChunk, serverChunk);
        }
    }

    @WrapOperation(method = "extractRenderState", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;ZI)V"))
    private void speedrunport$classicColumns(DebugScreenOverlay overlay, GuiGraphicsExtractor graphics, List<String> lines,
                                             boolean alignLeft, int screenWidth, Operation<Void> original) {
        if (!minecraft.debugEntries.isOverlayVisible()) {
            original.call(overlay, graphics, lines, alignLeft, screenWidth);
            return;
        }
        speedrunport$pushGuiScale(graphics);
        original.call(overlay, graphics, speedrunport$column(lines, alignLeft), alignLeft, graphics.guiWidth());
        graphics.pose().popMatrix();
    }

    @WrapOperation(method = "extractRenderState", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;Z)V"))
    private void speedrunport$classicColumnsBefore26_3(DebugScreenOverlay overlay, GuiGraphicsExtractor graphics, List<String> lines,
                                                       boolean alignLeft, Operation<Void> original) {
        if (!minecraft.debugEntries.isOverlayVisible()) {
            original.call(overlay, graphics, lines, alignLeft);
            return;
        }
        speedrunport$pushGuiScale(graphics);
        original.call(overlay, graphics, speedrunport$column(lines, alignLeft), alignLeft);
        graphics.pose().popMatrix();
    }

    @WrapOperation(method = "extractRenderState", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debugchart/FpsDebugChart;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V"))
    private void speedrunport$classicFrameGraph(FpsDebugChart chart, GuiGraphicsExtractor graphics, int left, int width, int bottom,
                                                Operation<Void> original) {
        speedrunport$frameGraph(graphics);
    }

    @WrapOperation(method = "extractRenderState", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debugchart/FpsDebugChart;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void speedrunport$classicFrameGraphBefore26_3(FpsDebugChart chart, GuiGraphicsExtractor graphics, int left, int width,
                                                          Operation<Void> original) {
        speedrunport$frameGraph(graphics);
    }

    @WrapOperation(method = "extractRenderState", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debugchart/TpsDebugChart;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V"))
    private void speedrunport$classicTickGraph(TpsDebugChart chart, GuiGraphicsExtractor graphics, int left, int width, int bottom,
                                               Operation<Void> original) {
        speedrunport$tickGraph(graphics);
    }

    @WrapOperation(method = "extractRenderState", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/debugchart/TpsDebugChart;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void speedrunport$classicTickGraphBefore26_3(TpsDebugChart chart, GuiGraphicsExtractor graphics, int left, int width,
                                                         Operation<Void> original) {
        speedrunport$tickGraph(graphics);
    }

    @Unique
    private List<String> speedrunport$column(List<String> vanillaLines, boolean alignLeft) {
        try {
            return alignLeft
                    ? LeftColumn.build(minecraft, getLevel(), getClientChunk(), getServerChunk(), showProfilerChart(), showFpsCharts())
                    : RightColumn.build(minecraft, speedrunport$otherModLines);
        } catch (RuntimeException e) {
            if (!speedrunport$loggedFailure) {
                speedrunport$loggedFailure = true;
                SpeedrunPortClient.LOGGER.error("Failed to build the classic F3 column", e);
            }
            return vanillaLines;
        }
    }

    @Unique
    private void speedrunport$frameGraph(GuiGraphicsExtractor graphics) {
        speedrunport$pushGuiScale(graphics);
        int graphWidth = Math.min(ClassicGraphs.MAX_WIDTH, graphics.guiWidth() / 2);
        ClassicGraphs.frameTimes(graphics, font, frameTimeLogger, 0, graphWidth, graphics.guiHeight(),
                minecraft.options.framerateLimit().get());
        graphics.pose().popMatrix();
    }

    @Unique
    private void speedrunport$tickGraph(GuiGraphicsExtractor graphics) {
        speedrunport$pushGuiScale(graphics);
        int graphWidth = Math.min(ClassicGraphs.MAX_WIDTH, graphics.guiWidth() / 2);
        float millisPerTick = minecraft.level != null ? minecraft.level.tickRateManager().millisecondsPerTick() : 50.0F;
        ClassicGraphs.tickTimes(graphics, font, tickTimeLogger, graphics.guiWidth() - graphWidth, graphWidth, graphics.guiHeight(), millisPerTick);
        graphics.pose().popMatrix();
    }

    @Unique
    private static void speedrunport$pushGuiScale(GuiGraphicsExtractor graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().identity();
    }
}
