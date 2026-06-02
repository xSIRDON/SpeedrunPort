package dev.ncerdon.oldf3.render;

/** Pure, Minecraft-free numeric helpers for the debug overlay. */
public final class F3Format {
    private static final long BYTES_PER_MIB = 1024L * 1024L;

    private F3Format() {}

    /** Whole MiB (truncated) from a byte count. */
    public static long toMiB(long bytes) {
        return bytes / BYTES_PER_MIB;
    }

    /** Integer percentage of {@code part} relative to {@code whole}; 0 when whole is 0. */
    public static long percent(long part, long whole) {
        return whole == 0L ? 0L : part * 100L / whole;
    }
}
