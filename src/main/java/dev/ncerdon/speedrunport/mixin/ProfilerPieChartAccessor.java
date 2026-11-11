package dev.ncerdon.speedrunport.mixin;

import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProfilerPieChart.class)
public interface ProfilerPieChartAccessor {
    @Accessor("profilerTreePath")
    String speedrunport$getProfilerTreePath();

    @Accessor("profilerTreePath")
    void speedrunport$setProfilerTreePath(String path);
}
