package dev.ncerdon.oldf3;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ncerdon.oldf3.render.F3Format;
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
 * The 1.16.1 debug keys that 26.3 removed: F3 + F (render distance) and F3 + Q (key list).
 * They live in the Debug category of Controls, so they can be rebound like vanilla's.
 */
public final class OldF3Keys {
    /** SDL scancodes, which is what 26.3 key mappings store. */
    private static final int KEY_F = 9;
    private static final int KEY_Q = 20;

    public static final KeyMapping CYCLE_RENDER_DISTANCE = new KeyMapping(
            "key.oldf3.cycleRenderDistance", InputConstants.Type.KEYBOARD, KEY_F, KeyMapping.Category.DEBUG);
    public static final KeyMapping HELP = new KeyMapping(
            "key.oldf3.help", InputConstants.Type.KEYBOARD, KEY_Q, KeyMapping.Category.DEBUG);

    private OldF3Keys() {}

    /** F3 + F: one chunk further, or nearer while Shift is held. */
    public static void cycleRenderDistance(Minecraft mc, boolean reverse, Consumer<Component> feedback) {
        OptionInstance<Integer> renderDistance = mc.options.renderDistance();
        int min = 2;
        int max = 32;
        if (renderDistance.values() instanceof OptionInstance.IntRangeBase range) {
            min = range.minInclusive();
            max = range.maxInclusive();
        }
        int next = F3Format.stepRenderDistance(renderDistance.get(), reverse, min, max);
        renderDistance.set(next);
        // Saving also tells the server, which decides how many chunks it sends from this.
        mc.options.save();
        feedback.accept(Component.translatable("oldf3.debug.cycle_renderdistance.message", next));
    }

    /** F3 + Q: the 1.16.1 list in its order, then the combos 1.16.1 never had. */
    public static void showHelp(Minecraft mc, Consumer<Component> feedback) {
        Options options = mc.options;
        KeyMapping mod = options.keyDebugModifier;
        feedback.accept(Component.translatable("debug.help.message"));

        List<Entry> entries = List.of(
                entry(options.keyDebugReloadChunk, Component.translatable("oldf3.help.reload_chunks")),
                entry(options.keyDebugShowHitboxes, Component.translatable("oldf3.help.show_hitboxes")),
                entry(options.keyDebugCopyLocation, Component.translatable("oldf3.help.copy_location",
                        Objects.requireNonNullElse(combo(mod, options.keyDebugCrash), name(mod)))),
                entry(options.keyDebugClearChat, Component.translatable("oldf3.help.clear_chat")),
                entry(CYCLE_RENDER_DISTANCE, Component.translatable("oldf3.help.cycle_renderdistance")),
                entry(options.keyDebugShowChunkBorders, Component.translatable("oldf3.help.chunk_boundaries")),
                entry(options.keyDebugShowAdvancedTooltips, Component.translatable("oldf3.help.advanced_tooltips")),
                entry(options.keyDebugCopyRecreateCommand, Component.translatable("oldf3.help.inspect")),
                entry(options.keyDebugSpectate, Component.translatable("oldf3.help.creative_spectator")),
                entry(options.keyDebugFocusPause, Component.translatable("oldf3.help.pause_focus")),
                entry(HELP, Component.translatable("oldf3.help.help")),
                entry(options.keyDebugReloadResourcePacks, Component.translatable("oldf3.help.reload_resourcepacks")),
                new Entry(name(mod) + " + Esc", Component.translatable("oldf3.help.pause")),
                entry(options.keyDebugSwitchGameMode, Component.translatable("oldf3.help.gamemodes")),
                entry(options.keyDebugProfiling, Component.translatable("oldf3.help.profiling")),
                entry(options.keyDebugDumpDynamicTextures, Component.translatable("oldf3.help.dump_dynamic_textures")),
                entry(options.keyDebugDumpVersion, Component.translatable("oldf3.help.dump_version")),
                entry(options.keyDebugDebugOptions, Component.translatable("oldf3.help.debug_options")),
                entry(options.keyDebugPofilingChart, Component.translatable("oldf3.help.profiling_chart")),
                entry(options.keyDebugFpsCharts, Component.translatable("oldf3.help.fps_charts")),
                entry(options.keyDebugNetworkCharts, Component.translatable("oldf3.help.network_charts")),
                entry(options.keyDebugLightmapTexture, Component.translatable("oldf3.help.lightmap_texture")),
                entry(options.keyDebugSwitchTranslucencyMode, Component.translatable("oldf3.help.improved_transparency")));

        for (Entry entry : entries) {
            if (entry.keys() != null) {
                mc.gui.hud.getChat().addClientSystemMessage(
                        Component.translatable("oldf3.help.line", entry.keys(), entry.description()));
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

    private static Entry entry(KeyMapping key, Component description) {
        return new Entry(combo(Minecraft.getInstance().options.keyDebugModifier, key), description);
    }

    private record Entry(@Nullable String keys, Component description) {}
}
