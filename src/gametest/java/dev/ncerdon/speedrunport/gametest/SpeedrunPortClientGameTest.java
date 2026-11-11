package dev.ncerdon.speedrunport.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ncerdon.speedrunport.SpeedrunPortConfig;
import dev.ncerdon.speedrunport.borderless.BorderlessBounds;
import dev.ncerdon.speedrunport.borderless.BorderlessBounds.Rect;
import dev.ncerdon.speedrunport.borderless.BorderlessToggle;
import dev.ncerdon.speedrunport.compat.Compat;
import dev.ncerdon.speedrunport.f3.ClassicDebugKeys;
import dev.ncerdon.speedrunport.f3.RightColumn;
import dev.ncerdon.speedrunport.standardsettings.StandardSettings;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

// Runs the real client through F3, the borderless key and StandardSettings, with screenshots.
// ./gradlew :26.3:runClientGameTest (or :26.1: / :26.2:)
// Shift/Alt are held with InputConstants codes because TestInput.holdShift()/holdAlt() send GLFW
// codes, which are wrong on 26.3.
public class SpeedrunPortClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        TestInput input = context.getInput();
        try {
            // StandardSettings gets tested at the end, keep it out of the first world
            Files.deleteIfExists(StandardSettings.file());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // small enough to fit on a 1080p screen with the title bar
        input.resizeWindow(1280, 720);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestCompat.waitForChunksRender(singleplayer);
            singleplayer.getServer().runCommand("setblock ~2 ~ ~ minecraft:oak_stairs[facing=east,waterlogged=true]");
            singleplayer.getServer().runCommand("summon minecraft:cow ~4 ~ ~3");
            TestCompat.lookAt(context, -90.0F, 30.0F);
            context.waitTicks(20);
            // the recipe toast covers the right column in screenshots
            context.runOnClient(Compat::clearToasts);
            // F3 should ignore 26.3's debug gui scale and match the normal gui scale
            context.runOnClient(Compat::setDebugGuiScaleAuto);

            // Plain F3: classic text columns, no charts.
            input.pressKey(options -> options.keyDebugOverlay);
            context.waitTicks(5);
            check(context, mc -> mc.debugEntries.isOverlayVisible(), "F3 opens the overlay");
            check(context, mc -> !overlay(mc).showProfilerChart() && !overlay(mc).showFpsCharts(), "plain F3 shows no charts");
            check(context, mc -> RightColumn.build(mc, List.of()).stream().noneMatch(line -> line.startsWith("#")),
                    "block and fluid tags stay hidden unless turned on in F3 + F6");
            context.takeScreenshot("speedrunport-1-f3");
            input.pressKey(options -> options.keyDebugOverlay);
            context.waitTicks(2);
            check(context, mc -> !mc.debugEntries.isOverlayVisible(), "F3 again closes the overlay");

            // Shift+F3: the 1.16.1 profiler pie.
            input.holdKey(InputConstants.KEY_LSHIFT);
            input.pressKey(options -> options.keyDebugOverlay);
            input.releaseKey(InputConstants.KEY_LSHIFT);
            context.waitTicks(60);
            check(context, mc -> overlay(mc).showProfilerChart() && !overlay(mc).showFpsCharts(), "Shift+F3 shows only the pie");
            context.takeScreenshot("speedrunport-2-shift-f3-pie");
            input.pressKey(options -> options.keyDebugOverlay);
            context.waitTicks(2);

            // Alt+F3: the 1.16.1 FPS (left) and TPS (right) graphs.
            input.holdKey(InputConstants.KEY_LALT);
            input.pressKey(options -> options.keyDebugOverlay);
            input.releaseKey(InputConstants.KEY_LALT);
            context.waitTicks(100);
            check(context, mc -> overlay(mc).showFpsCharts() && !overlay(mc).showProfilerChart(), "Alt+F3 shows only the graphs");
            context.takeScreenshot("speedrunport-3-alt-f3-graphs");

            // F3 + F and Shift+F3 + F: render distance up and back down, without closing the overlay.
            int before = context.computeOnClient(mc -> mc.options.renderDistance().get());
            input.holdKey(options -> options.keyDebugModifier);
            input.pressKey(ClassicDebugKeys.CYCLE_RENDER_DISTANCE);
            input.releaseKey(options -> options.keyDebugModifier);
            context.waitTicks(2);
            int raised = context.computeOnClient(mc -> mc.options.renderDistance().get());
            if (raised != before + 1) {
                throw new AssertionError("F3 + F should raise render distance from " + before + " but it is " + raised);
            }
            check(context, mc -> mc.debugEntries.isOverlayVisible(), "F3 used as a modifier must not toggle the overlay");

            input.holdKey(options -> options.keyDebugModifier);
            input.holdKey(InputConstants.KEY_LSHIFT);
            input.pressKey(ClassicDebugKeys.CYCLE_RENDER_DISTANCE);
            input.releaseKey(InputConstants.KEY_LSHIFT);
            input.releaseKey(options -> options.keyDebugModifier);
            context.waitTicks(2);
            int lowered = context.computeOnClient(mc -> mc.options.renderDistance().get());
            if (lowered != before) {
                throw new AssertionError("Shift + F3 + F should lower render distance back to " + before + " but it is " + lowered);
            }

            // F3 + Q: the key list in chat.
            input.holdKey(options -> options.keyDebugModifier);
            input.pressKey(ClassicDebugKeys.HELP);
            input.releaseKey(options -> options.keyDebugModifier);
            context.waitTicks(5);
            context.takeScreenshot("speedrunport-4-f3-q-help");
            input.pressKey(options -> options.keyDebugOverlay);
            context.waitTicks(2);

            // Borderless window key: no border, the monitor plus one off-screen pixel, then back to the old window.
            // (the harness fakes resize events, so WindowProbe asks the real window)
            Rect windowed = context.computeOnClient(WindowProbe::windowRect);
            input.pressKey(BorderlessToggle.KEY);
            context.waitTicks(5);
            Rect monitor = context.computeOnClient(WindowProbe::monitorRect);
            Rect borderless = context.computeOnClient(WindowProbe::windowRect);
            check(context, mc -> !WindowProbe.hasBorder(mc), "the borderless key removes the window border");
            Rect expected = BorderlessBounds.padded(monitor, context.computeOnClient(WindowProbe::otherMonitorRects));
            if (!borderless.equals(expected)) {
                throw new AssertionError("Borderless window should be " + expected + " (monitor " + monitor + " plus one off-screen pixel) but is " + borderless);
            }
            check(context, mc -> SpeedrunPortConfig.borderlessWindow(), "borderless is remembered for next launch");

            input.pressKey(BorderlessToggle.KEY);
            context.waitTicks(5);
            Rect restored = context.computeOnClient(WindowProbe::windowRect);
            check(context, WindowProbe::hasBorder, "pressing it again brings the border back");
            if (!restored.equals(windowed)) {
                throw new AssertionError("Leaving borderless should restore " + windowed + " but the window is " + restored);
            }
            check(context, mc -> !SpeedrunPortConfig.borderlessWindow(), "turning it off is remembered too");
        }

        // StandardSettings: a new world resets the options from config/standardoptions.txt.
        Path standardFile = StandardSettings.file();
        try {
            Files.write(standardFile, List.of("maxFps:90", "chunkborders:true", "renderDistanceOnWorldJoin:7"));
            context.runOnClient(mc -> mc.options.framerateLimit().set(200));
            try (TestSingleplayerContext standardWorld = context.worldBuilder().create()) {
                // waitForChunksRender() never finishes after the render distance changes mid-join, so
                // wait for a chunk past the old distance (5) instead
                context.waitFor(mc -> mc.player != null && mc.level.getChunkSource()
                        .hasChunk(mc.player.chunkPosition().x() + 6, mc.player.chunkPosition().z()), 1200);
                check(context, mc -> mc.options.framerateLimit().get() == 90, "the standard maxFps is applied to the new world");
                check(context, mc -> mc.options.renderDistance().get() == 7, "renderDistanceOnWorldJoin is applied on join");
                check(context, mc -> mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_BORDERS), "chunkborders turns chunk borders on");
                boolean snapshot = standardWorld.getServer().computeOnServer(
                        server -> Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("standardoptions.txt")));
                if (!snapshot) {
                    throw new AssertionError("Expected: the world folder gets a standardoptions.txt snapshot");
                }

                context.runOnClient(mc -> mc.options.framerateLimit().set(120));
                context.runOnClient(StandardSettings::saveCurrent);
                List<String> saved = Files.readAllLines(standardFile);
                if (!saved.contains("maxFps:120") || !saved.contains("chunkborders:true") || saved.stream().anyMatch(l -> l.startsWith("version:"))) {
                    throw new AssertionError("/standardsettings save should store current options and keep the extras, got " + saved);
                }
                context.runOnClient(mc -> mc.debugEntries.setStatus(DebugScreenEntries.CHUNK_BORDERS, DebugScreenEntryStatus.NEVER));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                Files.deleteIfExists(standardFile);
            } catch (IOException ignored) {
            }
        }
    }

    private static DebugScreenOverlay overlay(Minecraft mc) {
        return mc.getDebugOverlay();
    }

    private static void check(ClientGameTestContext context, Predicate<Minecraft> condition, String what) {
        if (!context.computeOnClient(condition::test)) {
            throw new AssertionError("Expected: " + what);
        }
    }
}
