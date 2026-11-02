package dev.ncerdon.speedrunport.mixin;

import dev.ncerdon.speedrunport.f3.ClassicPieChart;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.util.profiling.ProfileResults;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the profiler pie the 1.16.1 way. Data collection and number-key navigation stay vanilla. */
@Mixin(ProfilerPieChart.class)
public abstract class ProfilerPieChartMixin {
    @Shadow @Final private Font font;
    @Shadow private @Nullable ProfileResults profilerPieChartResults;
    @Shadow private String profilerTreePath;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void speedrunport$classicPie(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, CallbackInfo ci) {
        ci.cancel();
        if (profilerPieChartResults != null) {
            ClassicPieChart.render(graphics, font, profilerPieChartResults, profilerTreePath);
        }
    }
}
