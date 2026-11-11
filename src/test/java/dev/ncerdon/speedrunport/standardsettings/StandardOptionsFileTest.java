package dev.ncerdon.speedrunport.standardsettings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardOptionsFileTest {
    @Test
    void parse_splitsOnTheFirstColonOnly() {
        Map<String, String> values = StandardOptionsFile.parse(List.of(
                "fov:0.5", "key_key.jump:key.keyboard.space", "lastServer:mc.example.com:25565", "garbage", ""));
        assertEquals("0.5", values.get("fov"));
        assertEquals("key.keyboard.space", values.get("key_key.jump"));
        assertEquals("mc.example.com:25565", values.get("lastServer"));
        assertEquals(3, values.size());
    }

    @Test
    void format_roundTrips() {
        List<String> lines = List.of("fov:0.5", "renderDistance:8", "chunkborders:true");
        assertEquals(lines, StandardOptionsFile.format(StandardOptionsFile.parse(lines)));
    }

    @Test
    void settingsOnly_dropsExtrasAndBookkeeping() {
        Map<String, String> values = StandardOptionsFile.parse(List.of(
                "version:4790", "fov:0.5", "chunkborders:true", "renderDistanceOnWorldJoin:12", "lastServer:x", "maxFps:260"));
        assertEquals(List.of("fov", "maxFps"), List.copyOf(StandardOptionsFile.settingsOnly(values).keySet()));
    }

    @Test
    void fromCurrentOptions_keepsOldExtrasButTakesCurrentSettings() {
        Map<String, String> current = StandardOptionsFile.parse(List.of("version:4790", "fov:0.75", "renderDistance:10"));
        Map<String, String> previous = StandardOptionsFile.parse(List.of("fov:0.5", "pieDirectory:root.render", "f1:false"));
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("fov", "0.75");
        expected.put("renderDistance", "10");
        expected.put("pieDirectory", "root.render");
        expected.put("f1", "false");
        assertEquals(expected, StandardOptionsFile.fromCurrentOptions(current, previous));
    }

    @Test
    void isEnabled_unlessToggledOff() {
        assertTrue(StandardOptionsFile.isEnabled(Map.of()));
        assertTrue(StandardOptionsFile.isEnabled(Map.of("toggleStandardSettings", "true")));
        assertFalse(StandardOptionsFile.isEnabled(Map.of("toggleStandardSettings", "false")));
    }

    @Test
    void pieDirectory_convertsBetweenDotsAndProfilerSeparators() {
        String profiler = "root\u001erender\u001elevel";
        assertEquals(profiler, StandardOptionsFile.pieDirectoryToProfilerPath("root.render.level"));
        assertEquals("root.render.level", StandardOptionsFile.profilerPathToPieDirectory(profiler));
    }

    @Test
    void resolve_followsASingleLinePathToASharedFile(@TempDir Path dir) throws IOException {
        Path shared = Files.writeString(dir.resolve("global.txt"), "fov:0.5\n");
        Path local = Files.writeString(dir.resolve("standardoptions.txt"), shared.toAbsolutePath() + "\n");
        assertEquals(shared.toAbsolutePath(), StandardOptionsFile.resolve(local));
        assertEquals("0.5", StandardOptionsFile.read(local).get("fov"));
    }

    @Test
    void resolve_keepsANormalSingleOptionFile(@TempDir Path dir) throws IOException {
        Path local = Files.writeString(dir.resolve("standardoptions.txt"), "fov:0.5\n");
        assertEquals(local, StandardOptionsFile.resolve(local));
    }

    @Test
    void write_goesThroughTheRedirect(@TempDir Path dir) throws IOException {
        Path shared = Files.writeString(dir.resolve("global.txt"), "fov:0.5\n");
        Path local = Files.writeString(dir.resolve("standardoptions.txt"), shared.toAbsolutePath() + "\n");
        StandardOptionsFile.write(local, Map.of("fov", "0.25"));
        assertEquals("fov:0.25", Files.readString(shared).strip());
        assertEquals(shared.toAbsolutePath().toString(), Files.readString(local).strip());
    }
}
