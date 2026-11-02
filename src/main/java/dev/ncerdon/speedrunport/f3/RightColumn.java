package dev.ncerdon.speedrunport.f3;

import dev.ncerdon.speedrunport.compat.Compat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAt;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.TypedInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the RIGHT debug column in the 1.16.1 layout: system info, then the targeted
 * block / fluid / entity, then any lines other mods add to the debug screen.
 */
public final class RightColumn {
    private static final double TARGET_RANGE = 20.0;

    private RightColumn() {}

    public static List<String> build(Minecraft mc, List<String> otherModLines) {
        List<String> lines = new ArrayList<>();

        long max = Runtime.getRuntime().maxMemory();
        long total = Runtime.getRuntime().totalMemory();
        long used = total - Runtime.getRuntime().freeMemory();
        lines.add(String.format(Locale.ROOT, "Java: %s %sbit",
                System.getProperty("java.version"), System.getProperty("sun.arch.data.model", "64")));
        lines.add(String.format(Locale.ROOT, "Mem: % 2d%% %03d/%03dMB",
                F3Format.percent(used, max), F3Format.toMiB(used), F3Format.toMiB(max)));
        lines.add(String.format(Locale.ROOT, "Allocated: % 2d%% %03dMB",
                F3Format.percent(total, max), F3Format.toMiB(total)));
        lines.add("");
        lines.add("CPU: " + Compat.cpuInfo());
        lines.add("");

        lines.addAll(Compat.gpuLines(mc.getWindow()));

        if (!mc.showOnlyReducedInfo()) {
            appendTargets(mc, lines);
        }

        if (!otherModLines.isEmpty()) {
            lines.add("");
            lines.addAll(otherModLines);
        }
        return lines;
    }

    private static void appendTargets(Minecraft mc, List<String> lines) {
        Entity camera = mc.getCameraEntity();
        Level level = mc.level;
        if (camera == null || level == null) {
            return;
        }

        HitResult blockHit = camera.pick(TARGET_RANGE, 0.0F, false);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) blockHit).getBlockPos();
            BlockState state = level.getBlockState(pos);
            lines.add("");
            lines.add(ChatFormatting.UNDERLINE + "Targeted Block: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            lines.add(state.typeHolder().getRegisteredName());
            addProperties(lines, state);
            addTagsIfEnabled(mc, lines, DebugScreenEntries.LOOKING_AT_BLOCK_TAGS, state);
        }

        // 1.16.1 ray-casts again including fluids, so a solid block shows up here as minecraft:empty.
        HitResult fluidHit = camera.pick(TARGET_RANGE, 0.0F, true);
        if (fluidHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) fluidHit).getBlockPos();
            FluidState state = level.getFluidState(pos);
            lines.add("");
            lines.add(ChatFormatting.UNDERLINE + "Targeted Fluid: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            lines.add(state.typeHolder().getRegisteredName());
            addProperties(lines, state);
            addTagsIfEnabled(mc, lines, DebugScreenEntries.LOOKING_AT_FLUID_TAGS, state);
        }

        Entity target = mc.crosshairPickEntity;
        if (target != null) {
            lines.add("");
            lines.add(ChatFormatting.UNDERLINE + "Targeted Entity");
            lines.add(target.typeHolder().getRegisteredName());
        }
    }

    /**
     * 26.3 blocks carry dozens of tags (1.16.1 had a handful), so like vanilla 26.3 they stay hidden
     * unless "Looking at block/fluid tags" is turned on in F3 + F6.
     */
    private static void addTagsIfEnabled(Minecraft mc, List<String> lines, Identifier entry, TypedInstance<?> state) {
        if (mc.debugEntries.isCurrentlyEnabled(entry)) {
            DebugEntryLookingAt.addTagEntries(lines, state);
        }
    }

    private static void addProperties(List<String> lines, StateHolder<?, ?> state) {
        state.getValues().forEach(entry -> lines.add(propertyLine(entry)));
    }

    private static String propertyLine(Property.Value<?> entry) {
        String value = entry.valueName();
        if (Boolean.TRUE.equals(entry.value())) {
            value = ChatFormatting.GREEN + value;
        } else if (Boolean.FALSE.equals(entry.value())) {
            value = ChatFormatting.RED + value;
        }
        return entry.property().getName() + ": " + value;
    }
}
