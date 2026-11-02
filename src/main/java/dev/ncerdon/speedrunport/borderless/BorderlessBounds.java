package dev.ncerdon.speedrunport.borderless;

import java.util.List;

/**
 * Where the borderless window goes. A window that covers a monitor exactly can be taken over by the
 * GPU driver as real fullscreen, and then always-on-top windows such as Ninjabrain Bot stop showing
 * over it. Growing it one pixel past an edge keeps it an ordinary window (26.3 does the same for its
 * own borderless mode, but always to the right, which shows on a monitor placed there).
 */
public final class BorderlessBounds {
    public record Rect(int x, int y, int width, int height) {
        boolean intersects(Rect other) {
            return x < other.x + other.width && other.x < x + width
                    && y < other.y + other.height && other.y < y + height;
        }
    }

    private BorderlessBounds() {}

    /**
     * The monitor grown by one pixel past the first edge (top, bottom, left, right) that no other
     * monitor touches, so the extra pixel is never visible. If every edge has a neighbour, the
     * monitor itself.
     */
    public static Rect padded(Rect monitor, List<Rect> otherMonitors) {
        int x = monitor.x();
        int y = monitor.y();
        int w = monitor.width();
        int h = monitor.height();
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
