package dev.ncerdon.speedrunport.mixin;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the key handler flip the chart toggles the way Shift+F3 / Alt+F3 did in 1.16.1. */
@Mixin(DebugScreenOverlay.class)
public interface DebugScreenOverlayAccessor {
    @Accessor("renderProfilerChart")
    void speedrunport$setRenderProfilerChart(boolean visible);

    @Accessor("renderFpsCharts")
    void speedrunport$setRenderFpsCharts(boolean visible);

    @Accessor("renderNetworkCharts")
    void speedrunport$setRenderNetworkCharts(boolean visible);

    @Accessor("renderLightmapTexture")
    void speedrunport$setRenderLightmapTexture(boolean visible);
}
