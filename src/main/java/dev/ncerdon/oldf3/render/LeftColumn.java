package dev.ncerdon.oldf3.render;

import com.google.common.collect.Maps;
import dev.ncerdon.oldf3.OldF3Keys;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.timeline.Timelines;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds the LEFT debug column in the 1.16.1 layout. Line order and wording follow 1.16.1;
 * every value comes from the running game.
 */
public final class LeftColumn {
    private static final Map<Heightmap.Types, String> HEIGHTMAP_NAMES = Maps.newEnumMap(Map.of(
            Heightmap.Types.WORLD_SURFACE_WG, "SW",
            Heightmap.Types.WORLD_SURFACE, "S",
            Heightmap.Types.OCEAN_FLOOR_WG, "OW",
            Heightmap.Types.OCEAN_FLOOR, "O",
            Heightmap.Types.MOTION_BLOCKING, "M",
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, "ML"));

    private LeftColumn() {}

    /**
     * @param level         the integrated server's level when in singleplayer, else the client level
     * @param clientChunk   the client chunk the camera is in, if loaded
     * @param serverChunk   the matching integrated-server chunk, once it has been fetched
     * @param pieVisible    whether the Shift+F3 profiler pie is showing
     * @param graphsVisible whether the Alt+F3 FPS/TPS graphs are showing
     */
    public static List<String> build(Minecraft mc, @Nullable Level level, @Nullable LevelChunk clientChunk,
                                     @Nullable LevelChunk serverChunk, boolean pieVisible, boolean graphsVisible) {
        List<String> lines = new ArrayList<>();
        ClientLevel clientLevel = mc.level;
        Entity camera = mc.getCameraEntity();

        lines.add("Minecraft " + SharedConstants.getCurrentVersion().name()
                + " (" + mc.getLaunchedVersion() + "/" + ClientBrandRetriever.getClientModName() + ")");
        lines.add(fpsLine(mc));
        addIfPresent(lines, serverLine(mc, level));
        addIfPresent(lines, mc.levelExtractor.sectionStatistics());
        addIfPresent(lines, mc.levelExtractor.entityStatistics());

        if (clientLevel == null || camera == null) {
            addFooter(lines, mc, pieVisible, graphsVisible);
            return lines;
        }

        lines.add("P: " + F3Format.particleTotal(mc.particleEngine.countParticles()) + ". T: " + clientLevel.getEntityCount());
        lines.add(clientLevel.gatherChunkSourceStats());
        BlockPos pos = camera.blockPosition();

        if (mc.showOnlyReducedInfo()) {
            lines.add("");
            lines.add(String.format(Locale.ROOT, "Chunk-relative: %d %d %d", pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
            addFooter(lines, mc, pieVisible, graphsVisible);
            return lines;
        }

        ServerLevel serverLevel = level instanceof ServerLevel server ? server : null;
        if (serverLevel != null) {
            lines.add(serverLevel.gatherChunkSourceStats());
        }
        LongSet forcedChunks = serverLevel != null ? serverLevel.getForceLoadedChunks() : LongSets.EMPTY_SET;
        lines.add(clientLevel.dimension().identifier() + " FC: " + forcedChunks.size());
        lines.add("");

        ChunkPos chunkPos = ChunkPos.containing(pos);
        Direction facing = camera.getDirection();
        lines.add(String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f", camera.getX(), camera.getY(), camera.getZ()));
        lines.add(String.format(Locale.ROOT, "Block: %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
        lines.add(String.format(Locale.ROOT, "Chunk: %d %d %d in %d %d %d",
                pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15,
                chunkPos.x(), SectionPos.blockToSectionCoord(pos.getY()), chunkPos.z()));
        lines.add(String.format(Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)",
                facing, towards(facing), Mth.wrapDegrees(camera.getYRot()), Mth.wrapDegrees(camera.getXRot())));

        guarded(lines, () -> addWorldInfo(lines, mc, clientLevel, level, serverLevel, clientChunk, serverChunk, pos));

        List<Identifier> postEffects = mc.gameRenderer.getAppliedPostEffects();
        if (!postEffects.isEmpty()) {
            lines.add("Shader: " + postEffects.stream().map(Identifier::toString).collect(Collectors.joining(", ")));
        }
        if (mc.player != null) {
            lines.add(mc.getSoundManager().getChannelDebugString()
                    + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(mc.player.getCurrentMood() * 100.0F)));
        }

        addFooter(lines, mc, pieVisible, graphsVisible);
        return lines;
    }

    /** Light, heightmaps, biome, local difficulty and mob caps: the block 1.16.1 shows under "Facing". */
    private static void addWorldInfo(List<String> lines, Minecraft mc, ClientLevel clientLevel, @Nullable Level level,
                                     @Nullable ServerLevel serverLevel, @Nullable LevelChunk clientChunk,
                                     @Nullable LevelChunk serverChunk, BlockPos pos) {
        if (!clientLevel.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            lines.add("Outside of world...");
            return;
        }
        if (clientChunk == null || clientChunk.isEmpty()) {
            lines.add("Waiting for chunk...");
            return;
        }

        int rawLight = clientLevel.getChunkSource().getLightEngine().getRawBrightness(pos, 0);
        int sky = clientLevel.getBrightness(LightLayer.SKY, pos);
        int block = clientLevel.getBrightness(LightLayer.BLOCK, pos);
        lines.add("Client Light: " + rawLight + " (" + sky + " sky, " + block + " block)");
        if (serverChunk != null) {
            LevelLightEngine serverLight = serverChunk.getLevel().getLightEngine();
            lines.add("Server Light: (" + serverLight.getLayerListener(LightLayer.SKY).getLightValue(pos) + " sky, "
                    + serverLight.getLayerListener(LightLayer.BLOCK).getLightValue(pos) + " block)");
        } else {
            lines.add("Server Light: (?? sky, ?? block)");
        }

        StringBuilder heightmaps = new StringBuilder("CH");
        for (Heightmap.Types type : Heightmap.Types.values()) {
            if (type.sendToClient()) {
                heightmaps.append(' ').append(HEIGHTMAP_NAMES.get(type)).append(": ")
                        .append(clientChunk.getHeight(type, pos.getX(), pos.getZ()));
            }
        }
        lines.add(heightmaps.toString());
        heightmaps.setLength(0);
        heightmaps.append("SH");
        for (Heightmap.Types type : Heightmap.Types.values()) {
            if (type.keepAfterWorldgen()) {
                heightmaps.append(' ').append(HEIGHTMAP_NAMES.get(type)).append(": ")
                        .append(serverChunk != null ? Integer.toString(serverChunk.getHeight(type, pos.getX(), pos.getZ())) : "??");
            }
        }
        lines.add(heightmaps.toString());

        if (clientLevel.isInsideBuildHeight(pos.getY())) {
            lines.add("Biome: " + biomeName(clientLevel.getBiome(pos)));

            // Like 1.16.1: use the server's numbers when we have them, otherwise the client's best guess.
            Level difficultyLevel = level != null ? level : clientLevel;
            long inhabitedTime = serverChunk != null ? serverChunk.getInhabitedTime() : 0L;
            float moonBrightness = serverLevel != null && serverChunk != null ? serverLevel.getMoonBrightness(pos) : 0.0F;
            DifficultyInstance difficulty = new DifficultyInstance(difficultyLevel.getDifficulty(),
                    difficultyLevel.getOverworldClockTime(), inhabitedTime, moonBrightness);
            lines.add(String.format(Locale.ROOT, "Local Difficulty: %.2f // %.2f (Day %d)",
                    difficulty.getEffectiveDifficulty(), difficulty.getSpecialMultiplier(), dayCount(clientLevel)));
        }

        if (serverLevel != null) {
            NaturalSpawner.SpawnState spawnState = serverLevel.getChunkSource().getLastSpawnState();
            if (spawnState != null) {
                Object2IntMap<MobCategory> counts = spawnState.getMobCategoryCounts();
                lines.add("SC: " + spawnState.getSpawnableChunkCount() + ", " + Stream.of(MobCategory.values())
                        .map(category -> category.getDebugAbbreviation() + ": " + counts.getInt(category))
                        .collect(Collectors.joining(", ")));
            } else {
                lines.add("SC: N/A");
            }
        }
    }

    private static String fpsLine(Minecraft mc) {
        Options options = mc.options;
        int limit = options.framerateLimit().get();
        CloudStatus clouds = options.cloudStatus().get();
        return String.format(Locale.ROOT, "%d fps T: %s%s %s%s B: %d",
                mc.getFps(),
                limit >= 260 ? "inf" : Integer.toString(limit),
                options.enableVsync().get() ? " vsync" : "",
                options.graphicsPreset().get().getSerializedName(),
                clouds == CloudStatus.OFF ? "" : (clouds == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"),
                options.biomeBlendRadius().get());
    }

    private static @Nullable String serverLine(Minecraft mc, @Nullable Level level) {
        ClientPacketListener listener = mc.getConnection();
        if (listener == null) {
            return null;
        }
        Connection connection = listener.getConnection();
        float sent = connection.getAverageSentPackets();
        float received = connection.getAverageReceivedPackets();
        String status = level != null ? tickStatus(mc, level.tickRateManager()) : "";
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            return String.format(Locale.ROOT, "Integrated server @ %.0f ms ticks%s, %.0f tx, %.0f rx",
                    server.getCurrentSmoothedTickTime(), status, sent, received);
        }
        return String.format(Locale.ROOT, "\"%s\" server%s, %.0f tx, %.0f rx", listener.serverBrand(), status, sent, received);
    }

    /** Modern /tick state, shown only when it is not the normal 20 TPS run (1.16.1 had no such state). */
    private static String tickStatus(Minecraft mc, TickRateManager tickRate) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null && server.tickRateManager().isSprinting()) {
            return " (sprinting)";
        }
        if (tickRate.isSteppingForward()) {
            return " (frozen - stepping)";
        }
        return tickRate.isFrozen() ? " (frozen)" : "";
    }

    private static long dayCount(ClientLevel level) {
        return level.registryAccess().get(Timelines.OVERWORLD_DAY)
                .map(timeline -> (long) timeline.value().getPeriodCount(level.clockManager()))
                .orElseGet(() -> level.getOverworldClockTime() / 24000L);
    }

    private static String biomeName(Holder<Biome> biome) {
        return biome.unwrap().map(key -> key.identifier().toString(), value -> "[unregistered " + value + "]");
    }

    private static void addFooter(List<String> lines, Minecraft mc, boolean pieVisible, boolean graphsVisible) {
        boolean hasServer = mc.getSingleplayerServer() != null;
        lines.add("");
        lines.add("Debug: Pie [shift]: " + (pieVisible ? "visible" : "hidden")
                + (hasServer ? " FPS + TPS" : " FPS") + " [alt]: " + (graphsVisible ? "visible" : "hidden"));
        String help = OldF3Keys.combo(mc.options.keyDebugModifier, OldF3Keys.HELP);
        if (help != null) {
            lines.add("For help: press " + help);
        }
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

    private static void addIfPresent(List<String> lines, @Nullable String line) {
        if (line != null && !line.isEmpty()) {
            lines.add(line);
        }
    }

    /** One failing data source must never blank the whole overlay or crash the HUD. */
    private static void guarded(List<String> lines, Runnable section) {
        int size = lines.size();
        try {
            section.run();
        } catch (RuntimeException e) {
            lines.subList(size, lines.size()).clear();
            lines.add("[old-f3] " + e.getClass().getSimpleName());
        }
    }
}
