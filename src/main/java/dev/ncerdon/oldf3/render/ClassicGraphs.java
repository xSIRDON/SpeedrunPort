package dev.ncerdon.oldf3.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.debugchart.SampleStorage;
import net.minecraft.util.debugchart.TpsDebugDimensions;

import java.util.function.IntToLongFunction;

/**
 * The 1.16.1 Alt+F3 graphs: frame times bottom-left, integrated-server tick times bottom-right.
 * One 1px bar per sample, green to yellow to red, with the 60/30 FPS and 20 TPS guide lines.
 */
public final class ClassicGraphs {
    /** 1.16.1 kept 240 samples, so a graph is never wider than this. */
    public static final int MAX_WIDTH = 240;

    private static final int HEIGHT = 60;
    private static final int BACKGROUND = 0x90505050;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int FRAMERATE_LIMIT_LINE = 0xFF00FFFF;
    private static final double MILLIS_AT_60_FPS = 1000.0 / 60.0;

    private ClassicGraphs() {}

    public static void frameTimes(GuiGraphicsExtractor graphics, Font font, SampleStorage frames,
                                  int x, int width, int bottom, int framerateLimit) {
        Stats stats = bars(graphics, frames, frames::get, x, width, bottom, MILLIS_AT_60_FPS, 30, 100);
        guide(graphics, font, "60 FPS", x, width, bottom - 30);
        guide(graphics, font, "30 FPS", x, width, bottom - HEIGHT);
        border(graphics, x, width, bottom);
        if (framerateLimit > 0 && framerateLimit <= 250) {
            graphics.horizontalLine(x, x + width - 1, bottom - 1 - (int) (1800.0 / framerateLimit), FRAMERATE_LIMIT_LINE);
        }
        stats.draw(graphics, font, x, width, bottom);
    }

    public static void tickTimes(GuiGraphicsExtractor graphics, Font font, SampleStorage ticks,
                                 int x, int width, int bottom, float millisPerTick) {
        // 1.16.1 graphed how long the server's tick method ran; 26.3 logs exactly that as TICK_SERVER_METHOD.
        // (The full sample is the whole gap between ticks, idle wait included, so it is always ~50ms.)
        IntToLongFunction tickMethod = index -> ticks.get(index, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
        Stats stats = bars(graphics, ticks, tickMethod, x, width, bottom, millisPerTick, HEIGHT, HEIGHT);
        guide(graphics, font, F3Format.ticksPerSecond(millisPerTick) + " TPS", x, width, bottom - HEIGHT);
        border(graphics, x, width, bottom);
        stats.draw(graphics, font, x, width, bottom);
    }

    private static Stats bars(GuiGraphicsExtractor graphics, SampleStorage samples, IntToLongFunction value,
                              int x, int width, int bottom, double targetMillis, int pixelsAtTarget, int redHeight) {
        graphics.fill(x, bottom - HEIGHT, x + width, bottom, BACKGROUND);
        int count = Math.min(width, samples.size());
        int first = samples.size() - count;
        Stats stats = new Stats();
        for (int i = 0; i < count; i++) {
            long nanos = value.applyAsLong(first + i);
            stats.add(nanos / 1_000_000L);
            int height = F3Format.sampleHeight(nanos, targetMillis, pixelsAtTarget);
            // Bars may poke out above the box on lag spikes, exactly like 1.16.1.
            if (height > 1) {
                graphics.fill(x + i, bottom - height + 1, x + i + 1, bottom, F3Format.barColor(height, redHeight));
            }
        }
        return stats;
    }

    private static void guide(GuiGraphicsExtractor graphics, Font font, String label, int x, int width, int y) {
        graphics.fill(x + 1, y + 1, x + 14, y + 10, BACKGROUND);
        graphics.text(font, label, x + 2, y + 2, TEXT, false);
        graphics.horizontalLine(x, x + width - 1, y, WHITE);
    }

    private static void border(GuiGraphicsExtractor graphics, int x, int width, int bottom) {
        graphics.horizontalLine(x, x + width - 1, bottom - 1, WHITE);
        graphics.verticalLine(x, bottom - HEIGHT, bottom, WHITE);
        graphics.verticalLine(x + width - 1, bottom - HEIGHT, bottom, WHITE);
    }

    /** Whole-millisecond min / avg / max over the samples on screen. */
    private static final class Stats {
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;
        private long sum;
        private int count;

        void add(long millis) {
            min = Math.min(min, millis);
            max = Math.max(max, millis);
            sum += millis;
            count++;
        }

        void draw(GuiGraphicsExtractor graphics, Font font, int x, int width, int bottom) {
            if (count == 0) {
                return;
            }
            int y = bottom - HEIGHT - 9;
            String minText = min + " ms min";
            String avgText = sum / count + " ms avg";
            String maxText = max + " ms max";
            graphics.text(font, minText, x + 2, y, TEXT, true);
            graphics.text(font, avgText, x + width / 2 - font.width(avgText) / 2, y, TEXT, true);
            graphics.text(font, maxText, x + width - font.width(maxText), y, TEXT, true);
        }
    }
}
