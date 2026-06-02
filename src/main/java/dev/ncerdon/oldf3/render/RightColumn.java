package dev.ncerdon.oldf3.render;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the RIGHT debug column in the classic 1.16.1 layout, populated with
 * current 1.21.11 data.
 */
public final class RightColumn {
    private RightColumn() {}

    public static List<String> build(MinecraftClient client) {
        List<String> lines = new ArrayList<>();

        long max = Runtime.getRuntime().maxMemory();
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = total - free;

        lines.add(String.format(Locale.ROOT, "Java: %s 64bit", System.getProperty("java.version")));
        lines.add(String.format(Locale.ROOT, "Mem: %2d%% %03d/%03dMB",
                F3Format.percent(used, max), F3Format.toMiB(used), F3Format.toMiB(max)));
        lines.add(String.format(Locale.ROOT, "Allocated: %2d%% %03dMB",
                F3Format.percent(total, max), F3Format.toMiB(total)));
        lines.add("");
        lines.add("CPU: " + GLX._getCpuInfo());
        lines.add("");

        GpuDevice gpu = RenderSystem.getDevice();
        lines.add(String.format(Locale.ROOT, "Display: %dx%d (%s)",
                client.getWindow().getFramebufferWidth(),
                client.getWindow().getFramebufferHeight(),
                gpu.getVendor()));
        lines.add(gpu.getRenderer());
        lines.add(String.format(Locale.ROOT, "%s %s", gpu.getBackendName(), gpu.getVersion()));

        appendTargetedBlock(client, lines);
        return lines;
    }

    private static void appendTargetedBlock(MinecraftClient client, List<String> lines) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null) {
            return;
        }
        HitResult hit = camera.raycast(20.0, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = client.world.getBlockState(pos);

        lines.add("");
        lines.add("Targeted Block: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        lines.add(Registries.BLOCK.getId(state.getBlock()).toString());
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getEntries().entrySet()) {
            lines.add(entry.getKey().getName() + ": " + Util.getValueAsString(entry.getKey(), entry.getValue()));
        }
        state.streamTags().map(tag -> "#" + tag.id()).forEach(lines::add);
    }
}
