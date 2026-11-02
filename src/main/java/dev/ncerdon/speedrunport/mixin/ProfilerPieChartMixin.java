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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the profiler pie the 1.16.1 way. Data collection and number-key navigation stay vanilla. */
@Mixin(ProfilerPieChart.class)
public abstract class ProfilerPieChartMixin {
    @Shadow @Final private Font font;
    @Shadow private @Nullable ProfileResults profilerPieChartResults;
    @Shadow private String profilerTreePath;

    // 26.3 passes the screen size in; before that it only took the graphics. One handler each.

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V", require = 0,
            at = @At("HEAD"), cancellable = true)
    private void speedrunport$classicPie(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, CallbackInfo ci) {
        ci.cancel();
        speedrunport$render(graphics);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", require = 0,
            at = @At("HEAD"), cancellable = true)
    private void speedrunport$classicPieBefore26_3(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        ci.cancel();
        speedrunport$render(graphics);
    }

    @Unique
    private void speedrunport$render(GuiGraphicsExtractor graphics) {
        if (profilerPieChartResults != null) {
            ClassicPieChart.render(graphics, font, profilerPieChartResults, profilerTreePath);
        }
    }
}
