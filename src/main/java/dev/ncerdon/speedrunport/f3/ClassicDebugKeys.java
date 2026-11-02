package dev.ncerdon.speedrunport.f3;

import dev.ncerdon.speedrunport.compat.Compat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The 1.16.1 debug keys that 26.x removed: F3 + F (render distance) and F3 + Q (key list).
 * They live in the Debug category of Controls, so they can be rebound like vanilla's.
 */
public final class ClassicDebugKeys {
    public static final KeyMapping CYCLE_RENDER_DISTANCE = new KeyMapping(
            "key.speedrunport.cycleRenderDistance", Compat.KEYBOARD, Compat.KEY_F, KeyMapping.Category.DEBUG);
    public static final KeyMapping HELP = new KeyMapping(
            "key.speedrunport.help", Compat.KEYBOARD, Compat.KEY_Q, KeyMapping.Category.DEBUG);

    private ClassicDebugKeys() {}

    /** F3 + F: one chunk further, or nearer while Shift is held. */
    public static void cycleRenderDistance(Minecraft mc, boolean reverse, Consumer<Component> feedback) {
        OptionInstance<Integer> renderDistance = mc.options.renderDistance();
        int min = 2;
        int max = 32;
        int[] range = Compat.intRange(renderDistance);
        if (range != null) {
            min = range[0];
            max = range[1];
        }
        int next = F3Format.stepRenderDistance(renderDistance.get(), reverse, min, max);
        renderDistance.set(next);
        // Saving also tells the server, which decides how many chunks it sends from this.
        mc.options.save();
        feedback.accept(Component.translatable("speedrunport.debug.cycle_renderdistance.message", next));
    }

    /** F3 + Q: the 1.16.1 list in its order, then the combos 1.16.1 never had. */
    public static void showHelp(Minecraft mc, Consumer<Component> feedback) {
        Options options = mc.options;
        KeyMapping mod = options.keyDebugModifier;
        feedback.accept(Component.translatable("debug.help.message"));

        List<Entry> entries = List.of(
                entry(options.keyDebugReloadChunk, Component.translatable("speedrunport.help.reload_chunks")),
                entry(options.keyDebugShowHitboxes, Component.translatable("speedrunport.help.show_hitboxes")),
                entry(options.keyDebugCopyLocation, Component.translatable("speedrunport.help.copy_location",
                        Objects.requireNonNullElse(combo(mod, options.keyDebugCrash), name(mod)))),
                entry(options.keyDebugClearChat, Component.translatable("speedrunport.help.clear_chat")),
                entry(CYCLE_RENDER_DISTANCE, Component.translatable("speedrunport.help.cycle_renderdistance")),
                entry(options.keyDebugShowChunkBorders, Component.translatable("speedrunport.help.chunk_boundaries")),
                entry(options.keyDebugShowAdvancedTooltips, Component.translatable("speedrunport.help.advanced_tooltips")),
                entry(options.keyDebugCopyRecreateCommand, Component.translatable("speedrunport.help.inspect")),
                entry(options.keyDebugSpectate, Component.translatable("speedrunport.help.creative_spectator")),
                entry(options.keyDebugFocusPause, Component.translatable("speedrunport.help.pause_focus")),
                entry(HELP, Component.translatable("speedrunport.help.help")),
                entry(options.keyDebugReloadResourcePacks, Component.translatable("speedrunport.help.reload_resourcepacks")),
                new Entry(name(mod) + " + Esc", Component.translatable("speedrunport.help.pause")),
                entry(options.keyDebugSwitchGameMode, Component.translatable("speedrunport.help.gamemodes")),
                entry(options.keyDebugProfiling, Component.translatable("speedrunport.help.profiling")),
                entry(options.keyDebugDumpDynamicTextures, Component.translatable("speedrunport.help.dump_dynamic_textures")),
                entry(options.keyDebugDumpVersion, Component.translatable("speedrunport.help.dump_version")),
                entry(options.keyDebugDebugOptions, Component.translatable("speedrunport.help.debug_options")),
                entry(options.keyDebugPofilingChart, Component.translatable("speedrunport.help.profiling_chart")),
                entry(options.keyDebugFpsCharts, Component.translatable("speedrunport.help.fps_charts")),
                entry(options.keyDebugNetworkCharts, Component.translatable("speedrunport.help.network_charts")),
                entry(options.keyDebugLightmapTexture, Component.translatable("speedrunport.help.lightmap_texture")),
                entry(Compat.translucencyKey(options), Component.translatable("speedrunport.help.improved_transparency")));

        for (Entry entry : entries) {
            if (entry.keys() != null) {
                Compat.chat(mc).addClientSystemMessage(
                        Component.translatable("speedrunport.help.line", entry.keys(), entry.description()));
            }
        }
    }

    /** "F3 + Q", following the player's bindings; null when the action key is unbound. */
    public static @Nullable String combo(KeyMapping modifier, KeyMapping key) {
        return key.isUnbound() ? null : name(modifier) + " + " + name(key);
    }

    private static String name(KeyMapping key) {
        return key.getTranslatedKeyMessage().getString();
    }

    private static Entry entry(@Nullable KeyMapping key, Component description) {
        if (key == null) {
            return new Entry(null, description);
        }
        return new Entry(combo(Minecraft.getInstance().options.keyDebugModifier, key), description);
    }

    private record Entry(@Nullable String keys, Component description) {}
}
