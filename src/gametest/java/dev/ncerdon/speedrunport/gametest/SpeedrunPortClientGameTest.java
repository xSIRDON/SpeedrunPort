package dev.ncerdon.speedrunport.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ncerdon.speedrunport.SpeedrunPortConfig;
import dev.ncerdon.speedrunport.borderless.BorderlessBounds;
import dev.ncerdon.speedrunport.borderless.BorderlessBounds.Rect;
import dev.ncerdon.speedrunport.borderless.BorderlessToggle;
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
import org.lwjgl.sdl.SDLStdinc;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Drives the real client through the 1.16.1 F3 behaviour (saving a screenshot of each state) and
 * the borderless window key.
 * Run with {@code ./gradlew runClientGameTest}.
 * Shift and Alt are held by their SDL key codes: TestInput.holdShift()/holdAlt() still send GLFW codes.
 */
public class SpeedrunPortClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        TestInput input = context.getInput();
        try {
            // StandardSettings is tested at the end; keep it out of the first world.
            Files.deleteIfExists(StandardSettings.file());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // Small enough that the window plus its title bar fits on a 1920x1080 monitor.
        input.resizeWindow(1280, 720);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            singleplayer.getServer().runCommand("setblock ~2 ~ ~ minecraft:oak_stairs[facing=east,waterlogged=true]");
            singleplayer.getServer().runCommand("summon minecraft:cow ~4 ~ ~3");
            input.lookAt(-90.0F, 30.0F);
            context.waitTicks(20);
            // The "new recipes" toast would cover the top of the right column in the screenshots.
            context.runOnClient(mc -> mc.gui.toastManager().clear());

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
            // (The test harness fakes window resize events, so this checks the real SDL window directly.)
            Rect windowed = context.computeOnClient(SpeedrunPortClientGameTest::windowRect);
            input.pressKey(BorderlessToggle.KEY);
            context.waitTicks(5);
            Rect monitor = context.computeOnClient(SpeedrunPortClientGameTest::monitorRect);
            Rect borderless = context.computeOnClient(SpeedrunPortClientGameTest::windowRect);
            check(context, mc -> !hasBorder(mc), "the borderless key removes the window border");
            Rect expected = BorderlessBounds.padded(monitor, context.computeOnClient(SpeedrunPortClientGameTest::otherMonitorRects));
            if (!borderless.equals(expected)) {
                throw new AssertionError("Borderless window should be " + expected + " (monitor " + monitor + " plus one off-screen pixel) but is " + borderless);
            }
            check(context, mc -> SpeedrunPortConfig.borderlessWindow(), "borderless is remembered for next launch");

            input.pressKey(BorderlessToggle.KEY);
            context.waitTicks(5);
            Rect restored = context.computeOnClient(SpeedrunPortClientGameTest::windowRect);
            check(context, SpeedrunPortClientGameTest::hasBorder, "pressing it again brings the border back");
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
                // Not waitForChunksRender(): it wants the full square of the new render distance, which
                // never fills after the mid-join change. A chunk past the old distance (5) proves it took.
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

    private static Rect windowRect(Minecraft mc) {
        long handle = mc.getWindow().handle();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowPosition(handle, x, y);
            SDLVideo.SDL_GetWindowSize(handle, w, h);
            return new Rect(x.get(0), y.get(0), w.get(0), h.get(0));
        }
    }

    private static Rect monitorRect(Minecraft mc) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            SDLVideo.SDL_GetDisplayBounds(SDLVideo.SDL_GetDisplayForWindow(mc.getWindow().handle()), bounds);
            return new Rect(bounds.x(), bounds.y(), bounds.w(), bounds.h());
        }
    }

    private static List<Rect> otherMonitorRects(Minecraft mc) {
        int current = SDLVideo.SDL_GetDisplayForWindow(mc.getWindow().handle());
        List<Rect> others = new ArrayList<>();
        IntBuffer displays = SDLVideo.SDL_GetDisplays();
        if (displays == null) {
            return others;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            for (int i = 0; i < displays.remaining(); i++) {
                if (displays.get(i) != current && SDLVideo.SDL_GetDisplayBounds(displays.get(i), bounds)) {
                    others.add(new Rect(bounds.x(), bounds.y(), bounds.w(), bounds.h()));
                }
            }
        } finally {
            SDLStdinc.SDL_free(displays);
        }
        return others;
    }

    private static boolean hasBorder(Minecraft mc) {
        return (SDLVideo.SDL_GetWindowFlags(mc.getWindow().handle()) & SDLVideo.SDL_WINDOW_BORDERLESS) == 0;
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
