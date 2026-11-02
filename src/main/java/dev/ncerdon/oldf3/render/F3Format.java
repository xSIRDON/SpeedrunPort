package dev.ncerdon.oldf3.render;

/** Pure, Minecraft-free helpers for the classic overlay, the 1.16.1 graphs, and the debug keys. */
public final class F3Format {
    private static final long BYTES_PER_MIB = 1024L * 1024L;

    public static final int GREEN = 0xFF00FF00;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int RED = 0xFFFF0000;

    private F3Format() {}

    /** Whole MiB (truncated) from a byte count. */
    public static long toMiB(long bytes) {
        return bytes / BYTES_PER_MIB;
    }

    /** Integer percentage of {@code part} relative to {@code whole}; 0 when whole is 0. */
    public static long percent(long part, long whole) {
        return whole == 0L ? 0L : part * 100L / whole;
    }

    /**
     * 1.16.1 bar height: a sample that takes exactly {@code targetMillis} is {@code pixelsAtTarget}
     * tall (60 FPS frame = 30px, 20 TPS tick = 60px).
     */
    public static int sampleHeight(long nanos, double targetMillis, int pixelsAtTarget) {
        if (targetMillis <= 0.0) {
            return 0;
        }
        return (int) (nanos / 1_000_000.0 / targetMillis * pixelsAtTarget);
    }

    /**
     * 1.16.1 bar color: green at 0, yellow at half of {@code redHeight}, red at {@code redHeight}
     * and above.
     */
    public static int barColor(int height, int redHeight) {
        int clamped = Math.max(0, Math.min(height, redHeight));
        int yellowHeight = redHeight / 2;
        if (clamped < yellowHeight) {
            return lerpColor(GREEN, YELLOW, (float) clamped / yellowHeight);
        }
        return lerpColor(YELLOW, RED, (float) (clamped - yellowHeight) / (redHeight - yellowHeight));
    }

    /** Per-channel ARGB interpolation, truncating like 1.16.1's {@code interpolateColor}. */
    public static int lerpColor(int from, int to, float delta) {
        int a = lerpChannel(from >>> 24, to >>> 24, delta);
        int r = lerpChannel(from >> 16 & 0xFF, to >> 16 & 0xFF, delta);
        int g = lerpChannel(from >> 8 & 0xFF, to >> 8 & 0xFF, delta);
        int b = lerpChannel(from & 0xFF, to & 0xFF, delta);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lerpChannel(int from, int to, float delta) {
        return (int) (from + delta * (to - from));
    }

    /** F3+F: one step up (or down with Shift), clamped to the slider's range like 1.16.1. */
    public static int stepRenderDistance(int current, boolean reverse, int min, int max) {
        return Math.max(min, Math.min(current + (reverse ? -1 : 1), max));
    }

    /**
     * 26.3's particle count is a per-type breakdown ending in the total ("SO 16 T 16");
     * 1.16.1 showed only the total.
     */
    public static String particleTotal(String breakdown) {
        String trimmed = breakdown.strip();
        String last = trimmed.substring(trimmed.lastIndexOf(' ') + 1);
        return !last.isEmpty() && last.chars().allMatch(Character::isDigit) ? last : trimmed;
    }

    /** Ticks per second shown on the TPS graph label, from the target milliseconds per tick. */
    public static int ticksPerSecond(float millisPerTick) {
        return millisPerTick <= 0.0F ? 0 : Math.round(1000.0F / millisPerTick);
    }
}
