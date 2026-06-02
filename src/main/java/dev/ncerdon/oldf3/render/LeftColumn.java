package dev.ncerdon.oldf3.render;

import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the LEFT debug column in the classic 1.16.1 layout, populated with
 * current 1.21.11 data. Order/wording follow 1.16.1; values use modern APIs.
 */
public final class LeftColumn {
    private LeftColumn() {}

    public static List<String> build(MinecraftClient client) {
        List<String> lines = new ArrayList<>();

        // Version line.
        lines.add("Minecraft " + SharedConstants.getGameVersion().name()
                + " (" + client.getGameVersion() + "/" + ClientBrandRetriever.getClientModName() + ")");

        // FPS line.
        int limit = client.getInactivityFpsLimiter().update();
        lines.add(String.format(Locale.ROOT, "%d fps T: %s%s",
                client.getCurrentFps(),
                limit == 260 ? "inf" : Integer.toString(limit),
                client.options.getEnableVsync().getValue() ? " vsync" : ""));

        // Chunk / entity / particle render stats (vanilla-provided strings).
        addIfPresent(lines, client.worldRenderer.getChunksDebugString());
        addIfPresent(lines, client.worldRenderer.getEntitiesDebugString());
        lines.add("P: " + client.particleManager.getDebugString());

        Entity camera = client.getCameraEntity();
        ClientWorld world = client.world;
        if (camera == null || world == null) {
            return lines; // Title screen / not in a world.
        }

        // Dimension, then the classic blank separator.
        lines.add(world.getRegistryKey().getValue().toString());
        lines.add("");

        BlockPos pos = camera.getBlockPos();
        ChunkPos chunkPos = new ChunkPos(pos);

        lines.add(String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f",
                camera.getX(), camera.getY(), camera.getZ()));
        lines.add(String.format(Locale.ROOT, "Block: %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
        lines.add(String.format(Locale.ROOT, "Chunk: %d %d %d in %d %d %d",
                pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15,
                chunkPos.x, ChunkSectionPos.getSectionCoord(pos.getY()), chunkPos.z));

        Direction facing = camera.getHorizontalFacing();
        lines.add(String.format(Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)",
                facing, towards(facing),
                MathHelper.wrapDegrees(camera.getYaw()), MathHelper.wrapDegrees(camera.getPitch())));

        // Biome (only within the world's height limit, matching vanilla).
        if (world.isInHeightLimit(pos.getY())) {
            String biome = world.getBiome(pos).getKeyOrValue()
                    .map(key -> key.getValue().toString(), value -> "[unregistered " + value + "]");
            lines.add("Biome: " + biome);
        }

        // Light.
        int clientLight = world.getChunkManager().getLightingProvider().getLight(pos, 0);
        int sky = world.getLightLevel(LightType.SKY, pos);
        int block = world.getLightLevel(LightType.BLOCK, pos);
        lines.add("Client Light: " + clientLight + " (" + sky + " sky, " + block + " block)");

        return lines;
    }

    private static String towards(Direction facing) {
        return switch (facing) {
            case NORTH -> "Towards negative Z";
            case SOUTH -> "Towards positive Z";
            case WEST -> "Towards negative X";
            case EAST -> "Towards positive X";
            default -> "Invalid";
        };
    }

    private static void addIfPresent(List<String> lines, String line) {
        if (line != null && !line.isEmpty()) {
            lines.add(line);
        }
    }
}
