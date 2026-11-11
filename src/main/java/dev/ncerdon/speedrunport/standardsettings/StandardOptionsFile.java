package dev.ncerdon.speedrunport.standardsettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StandardOptionsFile {
    public static final String FILE_NAME = "standardoptions.txt";

    public static final String ENABLED = "toggleStandardSettings";
    public static final String HITBOXES = "hitboxes";
    public static final String CHUNK_BORDERS = "chunkborders";
    public static final String PERSPECTIVE = "perspective";
    public static final String HIDE_GUI = "f1";
    public static final String PIE_DIRECTORY = "pieDirectory";
    public static final String FOV_ON_JOIN = "fovOnWorldJoin";
    public static final String RENDER_DISTANCE_ON_JOIN = "renderDistanceOnWorldJoin";
    public static final String SIMULATION_DISTANCE_ON_JOIN = "simulationDistanceOnWorldJoin";
    public static final String ENTITY_DISTANCE_ON_JOIN = "entityDistanceScalingOnWorldJoin";
    public static final String GUI_SCALE_ON_JOIN = "guiScaleOnWorldJoin";
    public static final String AUTO_F3_ESC = "autoF3Esc";
    public static final String AUTO_F3_ESC_DELAY = "firstAutoF3EscDelay";

    public static final Set<String> EXTRAS = Set.of(ENABLED, HITBOXES, CHUNK_BORDERS, PERSPECTIVE, HIDE_GUI,
            PIE_DIRECTORY, FOV_ON_JOIN, RENDER_DISTANCE_ON_JOIN, SIMULATION_DISTANCE_ON_JOIN,
            ENTITY_DISTANCE_ON_JOIN, GUI_SCALE_ON_JOIN, AUTO_F3_ESC, AUTO_F3_ESC_DELAY);

    public static final Set<String> NOT_SETTINGS = Set.of("version", "lastServer", "resourcePacks",
            "incompatibleResourcePacks", "tutorialStep", "joinedFirstServer", "skipMultiplayerWarning",
            "onboardAccessibility", "startedCleanly", "telemetryOptInExtra");

    private StandardOptionsFile() {}

    public static Path resolve(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank()).toList();
        if (lines.size() == 1) {
            try {
                Path target = Path.of(lines.getFirst().strip());
                if (Files.isRegularFile(target)) {
                    return target;
                }
            } catch (InvalidPathException ignored) {
            }
        }
        return file;
    }

    public static Map<String, String> read(Path file) throws IOException {
        return parse(Files.readAllLines(resolve(file), StandardCharsets.UTF_8));
    }

    public static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                values.put(line.substring(0, colon), line.substring(colon + 1));
            }
        }
        return values;
    }

    public static List<String> format(Map<String, String> values) {
        List<String> lines = new ArrayList<>(values.size());
        values.forEach((key, value) -> lines.add(key + ":" + value));
        return lines;
    }

    public static void write(Path file, Map<String, String> values) throws IOException {
        Path target = Files.exists(file) ? resolve(file) : file;
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.write(target, format(values), StandardCharsets.UTF_8);
    }

    public static Map<String, String> settingsOnly(Map<String, String> values) {
        Map<String, String> settings = new LinkedHashMap<>(values);
        settings.keySet().removeIf(key -> EXTRAS.contains(key) || NOT_SETTINGS.contains(key));
        return settings;
    }

    public static Map<String, String> fromCurrentOptions(Map<String, String> currentOptions, Map<String, String> previous) {
        Map<String, String> standard = settingsOnly(currentOptions);
        previous.forEach((key, value) -> {
            if (EXTRAS.contains(key)) {
                standard.put(key, value);
            }
        });
        return standard;
    }

    public static boolean isEnabled(Map<String, String> values) {
        return !"false".equals(values.getOrDefault(ENABLED, "true").strip());
    }

    public static String pieDirectoryToProfilerPath(String pieDirectory) {
        return pieDirectory.strip().replace('.', '\u001e');
    }

    public static String profilerPathToPieDirectory(String profilerPath) {
        return profilerPath.replace('\u001e', '.');
    }
}
