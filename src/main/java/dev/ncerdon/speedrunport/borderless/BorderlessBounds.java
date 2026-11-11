package dev.ncerdon.speedrunport.borderless;

import java.util.List;

public final class BorderlessBounds {
    public record Rect(int x, int y, int width, int height) {
        boolean intersects(Rect other) {
            return x < other.x + other.width && other.x < x + width
                    && y < other.y + other.height && other.y < y + height;
        }
    }

    private BorderlessBounds() {}

    // A window that covers the monitor exactly gets treated as real fullscreen by the driver, and
    // always-on-top stuff (ninjabrain bot) stops showing over it. So make it 1px bigger than the
    // monitor, sticking out of an edge that has no other monitor next to it (vanilla 26.3 always
    // uses the right edge, which bleeds onto a second screen). Falls back to the exact monitor.
    public static Rect padded(Rect monitor, List<Rect> otherMonitors) {
        int x = monitor.x();
        int y = monitor.y();
        int w = monitor.width();
        int h = monitor.height();
        // {strip the extra pixel would land on, window}: top, bottom, left, right
        List<Rect[]> edges = List.of(
                new Rect[] {new Rect(x, y - 1, w, 1), new Rect(x, y - 1, w, h + 1)},
                new Rect[] {new Rect(x, y + h, w, 1), new Rect(x, y, w, h + 1)},
                new Rect[] {new Rect(x - 1, y, 1, h), new Rect(x - 1, y, w + 1, h)},
                new Rect[] {new Rect(x + w, y, 1, h), new Rect(x, y, w + 1, h)});
        for (Rect[] edge : edges) {
            Rect strip = edge[0];
            if (otherMonitors.stream().noneMatch(strip::intersects)) {
                return edge[1];
            }
        }
        return monitor;
    }
}
