package dev.ncerdon.oldf3.mixin;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the key handler flip the chart toggles the way Shift+F3 / Alt+F3 did in 1.16.1. */
@Mixin(DebugScreenOverlay.class)
public interface DebugScreenOverlayAccessor {
    @Accessor("renderProfilerChart")
    void oldf3$setRenderProfilerChart(boolean visible);

    @Accessor("renderFpsCharts")
    void oldf3$setRenderFpsCharts(boolean visible);

    @Accessor("renderNetworkCharts")
    void oldf3$setRenderNetworkCharts(boolean visible);

    @Accessor("renderLightmapTexture")
    void oldf3$setRenderLightmapTexture(boolean visible);
}
