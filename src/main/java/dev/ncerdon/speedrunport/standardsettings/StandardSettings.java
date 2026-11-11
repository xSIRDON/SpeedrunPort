package dev.ncerdon.speedrunport.standardsettings;

import dev.ncerdon.speedrunport.SpeedrunPortClient;
import dev.ncerdon.speedrunport.compat.Compat;
import dev.ncerdon.speedrunport.mixin.ProfilerPieChartAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.language.LanguageManager;
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
    private static @Nullable Map<String, String> waitingForFocus;
    private static @Nullable Map<String, String> pendingKeys;
    private static boolean autoPause;
    private static int pauseDelay;
    private static boolean firstWorldPaused;

    private static @Nullable Path lastWorld;
    private static @Nullable Path cachedWorld;
    private static @Nullable Map<String, String> cachedOptions;

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

    public static void onWorldLoad(Minecraft mc, Path world, boolean newWorld) {
        pendingJoin = null;
        pendingWorld = null;
        waitingForFocus = null;
        pendingKeys = null;
        autoPause = false;
        if (newWorld) {
            if (sameFolder(world, cachedWorld)) {
                cachedWorld = null;
                cachedOptions = null;
            }
            if (lastWorld != null && sameFolder(lastWorld, world)) {
                lastWorld = null;
            }
            onNewWorld(mc, world);
        } else {
            restoreCache(mc, world);
        }
        lastWorld = world;
    }

    private static void onNewWorld(Minecraft mc, Path world) {
        Map<String, String> standard = read();
        if (standard == null || !isEnabled(standard)) {
            return;
        }
        try {
            cacheLastWorld(mc, standard);
            applyOptions(mc, settingsOnly(standard));
            applyExtras(mc, standard);
            writeSnapshot(mc, standard, world);
            pendingJoin = standard;
            pendingWorld = world;
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

        String pie = standard.get(PIE_DIRECTORY);
        if (pie != null && !pie.isBlank()) {
            ((ProfilerPieChartAccessor) mc.getDebugOverlay().getProfilerPieChart())
                    .speedrunport$setProfilerTreePath(pieDirectoryToProfilerPath(pie));
        }
        if (standard.containsKey(SNEAKING) || standard.containsKey(SPRINTING)) {
            pendingKeys = standard;
        }
        if (isTrue(standard.get(AUTO_F3_ESC))) {
            autoPause = !mc.isWindowActive();
            pauseDelay = firstWorldPaused ? 0 : parseInt(standard.get(AUTO_F3_ESC_DELAY), DEFAULT_AUTO_F3_ESC_DELAY);
            firstWorldPaused = true;
        }
        if (mc.isWindowActive()) {
            applyOnJoin(mc, standard);
        } else {
            waitingForFocus = standard;
        }
    }

    public static void onTick(Minecraft mc) {
        if (mc.level == null) {
            waitingForFocus = null;
            pendingKeys = null;
            autoPause = false;
            return;
        }
        if (waitingForFocus != null && mc.isWindowActive()) {
            applyOnJoin(mc, waitingForFocus);
        }
        if (pendingKeys != null && Compat.screen(mc) == null) {
            applyKeys(mc, pendingKeys);
            pendingKeys = null;
        }
        if (autoPause) {
            if (mc.isWindowActive() || mc.getSingleplayerServer() == null || Compat.screen(mc) instanceof PauseScreen) {
                autoPause = false;
            } else if (pauseDelay > 0) {
                pauseDelay--;
            } else {
                mc.pauseGame(true);
                autoPause = false;
            }
        }
    }

    public static void onResize(Minecraft mc) {
        if (waitingForFocus != null && isTrue(waitingForFocus.get(CHANGE_ON_RESIZE))) {
            applyOnJoin(mc, waitingForFocus);
        }
    }

    public static boolean applyNow(Minecraft mc) throws IOException {
        Map<String, String> standard = read();
        if (standard == null) {
            return false;
        }
        applyOptions(mc, settingsOnly(standard));
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

    private static void applyOnJoin(Minecraft mc, Map<String, String> standard) {
        waitingForFocus = null;
        Options options = mc.options;
        setNumber(options.fov(), standard.get(FOV_ON_JOIN), value -> (int) Math.round(fovDegrees(value.doubleValue())));
        setNumber(options.renderDistance(), standard.get(RENDER_DISTANCE_ON_JOIN), Number::intValue);
        setNumber(options.simulationDistance(), standard.get(SIMULATION_DISTANCE_ON_JOIN), Number::intValue);
        setNumber(options.entityDistanceScaling(), standard.get(ENTITY_DISTANCE_ON_JOIN), Number::doubleValue);
        setNumber(options.guiScale(), standard.get(GUI_SCALE_ON_JOIN), Number::intValue);
        options.save();
    }

    private static void applyKeys(Minecraft mc, Map<String, String> standard) {
        Options options = mc.options;
        setToggled(options.keyShift, options.toggleCrouch().get(), standard.get(SNEAKING));
        setToggled(options.keySprint, options.toggleSprint().get(), standard.get(SPRINTING));
    }

    private static void setToggled(KeyMapping key, boolean toggleMode, @Nullable String value) {
        if (value != null && toggleMode && key.isDown() != isTrue(value)) {
            key.setDown(true);
        }
    }

    private static void cacheLastWorld(Minecraft mc, Map<String, String> standard) throws IOException {
        if (lastWorld == null || sameFolder(lastWorld, cachedWorld)) {
            return;
        }
        mc.options.save();
        Map<String, String> current = readOptionsFile(mc);
        Map<String, String> cached = new LinkedHashMap<>();
        for (String key : settingsOnly(standard).keySet()) {
            String value = current.get(key);
            if (value != null) {
                cached.put(key, value);
            }
        }
        cachedOptions = cached;
        cachedWorld = lastWorld;
        SpeedrunPortClient.LOGGER.info("StandardSettings: cached the options of {}", folderName(lastWorld));
    }

    private static void restoreCache(Minecraft mc, Path world) {
        if (cachedOptions == null || !sameFolder(world, cachedWorld)) {
            return;
        }
        try {
            applyOptions(mc, cachedOptions);
            SpeedrunPortClient.LOGGER.info("StandardSettings: restored the options of {}", folderName(world));
        } catch (IOException | RuntimeException e) {
            SpeedrunPortClient.LOGGER.error("StandardSettings: could not restore the options for {}", world, e);
        }
        cachedOptions = null;
        cachedWorld = null;
    }

    private static void applyOptions(Minecraft mc, Map<String, String> values) throws IOException {
        mc.options.save();
        Map<String, String> merged = readOptionsFile(mc);
        merged.putAll(values);
        Files.write(mc.options.getFile().toPath(), format(merged), StandardCharsets.UTF_8);
        mc.options.load();
        mc.options.save();

        LanguageManager languages = mc.getLanguageManager();
        if (!languages.getSelected().equals(mc.options.languageCode)) {
            languages.setSelected(mc.options.languageCode);
            languages.onResourceManagerReload(mc.getResourceManager());
        }
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
            Compat.setHudHidden(mc, isTrue(hideGui));
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
        return server != null && sameFolder(server.getWorldPath(LevelResource.ROOT), world);
    }

    private static boolean sameFolder(Path a, @Nullable Path b) {
        return b != null && a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
    }

    private static Path folderName(Path world) {
        return world.toAbsolutePath().normalize().getFileName();
    }

    private static Map<String, String> readOptionsFile(Minecraft mc) throws IOException {
        Path options = mc.options.getFile().toPath();
        return Files.exists(options)
                ? new LinkedHashMap<>(parse(Files.readAllLines(options, StandardCharsets.UTF_8)))
                : new LinkedHashMap<>();
    }

    private static void setDebugRenderer(Minecraft mc, Identifier entry, @Nullable String value) {
        if (value != null) {
            mc.debugEntries.setStatus(entry, isTrue(value) ? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
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

    private static boolean isTrue(@Nullable String value) {
        return value != null && "true".equals(value.strip());
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
