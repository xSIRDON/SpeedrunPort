package dev.ncerdon.speedrunport.borderless;

import dev.ncerdon.speedrunport.borderless.BorderlessBounds.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BorderlessBoundsTest {
    private static final Rect PRIMARY = new Rect(0, 0, 1920, 1080);

    @Test
    void singleMonitor_growsOffTheTop() {
        assertEquals(new Rect(0, -1, 1920, 1081), BorderlessBounds.padded(PRIMARY, List.of()));
    }

    @Test
    void sideBySide_neverSpillsOntoTheOtherMonitor() {
        Rect right = new Rect(1920, 0, 1920, 1080);
        assertEquals(new Rect(0, -1, 1920, 1081), BorderlessBounds.padded(PRIMARY, List.of(right)));
        assertEquals(new Rect(1920, -1, 1920, 1081), BorderlessBounds.padded(right, List.of(PRIMARY)));
    }

    @Test
    void monitorAbove_growsOffTheBottom() {
        Rect above = new Rect(0, -1080, 1920, 1080);
        assertEquals(new Rect(0, 0, 1920, 1081), BorderlessBounds.padded(PRIMARY, List.of(above)));
    }

    @Test
    void monitorsAboveAndBelow_growsOffTheLeft() {
        Rect above = new Rect(0, -1080, 1920, 1080);
        Rect below = new Rect(0, 1080, 1920, 1080);
        assertEquals(new Rect(-1, 0, 1921, 1080), BorderlessBounds.padded(PRIMARY, List.of(above, below)));
    }

    @Test
    void offsetNeighbourOnlyBlocksTheEdgeItTouches() {
        Rect aboveRight = new Rect(1000, -768, 1366, 768);
        assertEquals(new Rect(0, 0, 1920, 1081), BorderlessBounds.padded(PRIMARY, List.of(aboveRight)));
    }

    @Test
    void surrounded_fallsBackToTheExactMonitor() {
        List<Rect> around = List.of(
                new Rect(0, -1080, 1920, 1080), new Rect(0, 1080, 1920, 1080),
                new Rect(-1920, 0, 1920, 1080), new Rect(1920, 0, 1920, 1080));
        assertEquals(PRIMARY, BorderlessBounds.padded(PRIMARY, around));
    }
}
