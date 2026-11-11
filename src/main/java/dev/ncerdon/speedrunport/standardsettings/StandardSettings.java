package dev.ncerdon.speedrunport.standardsettings;

import dev.ncerdon.speedrunport.SpeedrunPortClient;
import dev.ncerdon.speedrunport.compat.Compat;
import dev.ncerdon.speedrunport.mixin.ProfilerPieChartAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static dev.ncerdon.speedrunport.standardsettings.StandardOptionsFile.*;

public final class StandardSettings {
    private static final int DEFAULT_AUTO_F3_ESC_DELAY = 22;

    private static @Nullable Map<String, String> pendingJoin;
    private static @Nullable Path pendingWorld;
    private static int autoPauseIn = -1;

    private StandardSettings() {}

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static @Nullable Map<String, String> read() {
        Path file = file();
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return StandardOptionsFile.read(file);
        } catch (IOException e) {
            SpeedrunPortClient.LOGGER.warn("Could not read {}", file, e);
            return null;
        }
    }

    public static void onNewWorld(Minecraft mc, Path worldFolder) {
        Map<String, String> standard = read();
        if (standard == null || !isEnabled(standard)) {
            return;
        }
        try {
            applySettings(mc, standard);
            applyExtras(mc, standard);
            writeSnapshot(mc, standard, worldFolder);
            pendingJoin = standard;
            pendingWorld = worldFolder;
        } catch (IOException | RuntimeException e) {
            SpeedrunPortClient.LOGGER.error("StandardSettings: could not apply {}", file(), e);
        }
    }

    public static void onJoin(Minecraft mc) {
        Map<String, String> standard = pendingJoin;
        Path world = pendingWorld;
        pendingJoin = null;
        pendingWorld = null;
        if (standard == null || !isWorld(mc, world)) {
            return;
        }
        Options options = mc.options;
        setNumber(options.fov(), standard.get(FOV_ON_JOIN), Number::intValue);
        setNumber(options.renderDistance(), standard.get(RENDER_DISTANCE_ON_JOIN), Number::intValue);
        setNumber(options.simulationDistance(), standard.get(SIMULATION_DISTANCE_ON_JOIN), Number::intValue);
        setNumber(options.entityDistanceScaling(), standard.get(ENTITY_DISTANCE_ON_JOIN), Number::doubleValue);
        setNumber(options.guiScale(), standard.get(GUI_SCALE_ON_JOIN), Number::intValue);
        options.save();

        String pie = standard.get(PIE_DIRECTORY);
        if (pie != null && !pie.isBlank()) {
            ((ProfilerPieChartAccessor) mc.getDebugOverlay().getProfilerPieChart())
                    .speedrunport$setProfilerTreePath(pieDirectoryToProfilerPath(pie));
        }
        if ("true".equals(standard.getOrDefault(AUTO_F3_ESC, "false").strip())) {
            autoPauseIn = parseInt(standard.get(AUTO_F3_ESC_DELAY), DEFAULT_AUTO_F3_ESC_DELAY);
        }
    }

    public static void onTick(Minecraft mc) {
        if (autoPauseIn < 0) {
            return;
        }
        if (mc.level == null) {
            autoPauseIn = -1;
        } else if (autoPauseIn-- == 0 && Compat.screen(mc) == null) {
            mc.pauseGame(true);
        }
    }

    public static boolean applyNow(Minecraft mc) throws IOException {
        Map<String, String> standard = read();
        if (standard == null) {
            return false;
        }
        applySettings(mc, standard);
        applyExtras(mc, standard);
        return true;
    }

    public static Map<String, String> saveCurrent(Minecraft mc) throws IOException {
        mc.options.save();
        Map<String, String> previous = Files.exists(file()) ? StandardOptionsFile.read(file()) : Map.of();
        Map<String, String> standard = fromCurrentOptions(readOptionsFile(mc), previous);
        String piePath = ((ProfilerPieChartAccessor) mc.getDebugOverlay().getProfilerPieChart()).speedrunport$getProfilerTreePath();
        if (piePath != null && !piePath.isEmpty() && !"root".equals(piePath)) {
            standard.put(PIE_DIRECTORY, profilerPathToPieDirectory(piePath));
        }
        StandardOptionsFile.write(file(), standard);
        return standard;
    }

    public static void setEnabled(boolean enabled) throws IOException {
        Map<String, String> standard = new LinkedHashMap<>(StandardOptionsFile.read(file()));
        standard.put(ENABLED, Boolean.toString(enabled));
        StandardOptionsFile.write(file(), standard);
    }

    private static void applySettings(Minecraft mc, Map<String, String> standard) throws IOException {
        mc.options.save();
        Map<String, String> merged = readOptionsFile(mc);
        merged.putAll(settingsOnly(standard));
        Files.write(mc.options.getFile().toPath(), format(merged), StandardCharsets.UTF_8);
        mc.options.load();
        mc.options.save();
    }

    private static void applyExtras(Minecraft mc, Map<String, String> standard) {
        setDebugRenderer(mc, DebugScreenEntries.ENTITY_HITBOXES, standard.get(HITBOXES));
        setDebugRenderer(mc, DebugScreenEntries.CHUNK_BORDERS, standard.get(CHUNK_BORDERS));
        String perspective = standard.get(PERSPECTIVE);
        if (perspective != null) {
            CameraType[] types = CameraType.values();
            mc.options.setCameraType(types[Math.floorMod(parseInt(perspective, 0), types.length)]);
        }
        String hideGui = standard.get(HIDE_GUI);
        if (hideGui != null) {
            Compat.setHudHidden(mc, "true".equals(hideGui.strip()));
        }
    }

    private static void writeSnapshot(Minecraft mc, Map<String, String> standard, Path worldFolder) throws IOException {
        Map<String, String> snapshot = settingsOnly(readOptionsFile(mc));
        standard.forEach((key, value) -> {
            if (EXTRAS.contains(key) && !ENABLED.equals(key)) {
                snapshot.put(key, value);
            }
        });
        Files.createDirectories(worldFolder);
        Files.write(worldFolder.resolve(FILE_NAME), format(snapshot), StandardCharsets.UTF_8);
    }

    private static boolean isWorld(Minecraft mc, @Nullable Path world) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || world == null) {
            return false;
        }
        return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize()
                .equals(world.toAbsolutePath().normalize());
    }

    private static Map<String, String> readOptionsFile(Minecraft mc) throws IOException {
        Path options = mc.options.getFile().toPath();
        return Files.exists(options)
                ? new LinkedHashMap<>(parse(Files.readAllLines(options, StandardCharsets.UTF_8)))
                : new LinkedHashMap<>();
    }

    private static void setDebugRenderer(Minecraft mc, Identifier entry, @Nullable String value) {
        if (value != null) {
            mc.debugEntries.setStatus(entry,
                    "true".equals(value.strip()) ? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
        }
    }

    private static <T> void setNumber(OptionInstance<T> option, @Nullable String value, Function<Number, T> convert) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            option.set(convert.apply(Double.parseDouble(value.strip())));
        } catch (NumberFormatException e) {
            SpeedrunPortClient.LOGGER.warn("StandardSettings: {} is not a number", value);
        }
    }

    private static int parseInt(@Nullable String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return (int) Double.parseDouble(value.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
