package dev.ncerdon.speedrunport.f3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class F3FormatTest {

    @Test
    void toMiB_truncatesToWholeMiB() {
        assertEquals(0L, F3Format.toMiB(0L));
        assertEquals(1L, F3Format.toMiB(1024L * 1024L));
        assertEquals(2L, F3Format.toMiB(2L * 1024L * 1024L + 5L));
    }

    @Test
    void percent_computesIntegerPercentage() {
        assertEquals(50L, F3Format.percent(512L, 1024L));
        assertEquals(100L, F3Format.percent(1024L, 1024L));
        assertEquals(0L, F3Format.percent(0L, 1024L));
    }

    @Test
    void percent_isZeroWhenWholeIsZero() {
        assertEquals(0L, F3Format.percent(123L, 0L));
    }

    @Test
    void percent_canExceed100() {
        // Mem line can briefly exceed 100% under GC pressure (total > max).
        assertEquals(200L, F3Format.percent(2048L, 1024L));
    }

    @Test
    void sampleHeight_matches116FrameScale() {
        // A 60 FPS frame (16.67ms) is 30px; a 30 FPS frame is 60px.
        assertEquals(30, F3Format.sampleHeight(16_666_667L, 1000.0 / 60.0, 30));
        assertEquals(60, F3Format.sampleHeight(33_333_334L, 1000.0 / 60.0, 30));
    }

    @Test
    void sampleHeight_matches116TickScale() {
        // A full 50ms tick fills the 60px graph; half a tick is half height.
        assertEquals(60, F3Format.sampleHeight(50_000_000L, 50.0, 60));
        assertEquals(30, F3Format.sampleHeight(25_000_000L, 50.0, 60));
    }

    @Test
    void sampleHeight_isZeroForInvalidTarget() {
        assertEquals(0, F3Format.sampleHeight(50_000_000L, 0.0, 60));
    }

    @Test
    void barColor_goesGreenYellowRed() {
        assertEquals(F3Format.GREEN, F3Format.barColor(0, 100));
        assertEquals(F3Format.YELLOW, F3Format.barColor(50, 100));
        assertEquals(F3Format.RED, F3Format.barColor(100, 100));
        assertEquals(F3Format.RED, F3Format.barColor(500, 100));
        assertEquals(F3Format.GREEN, F3Format.barColor(-5, 100));
    }

    @Test
    void barColor_blendsBetweenStops() {
        // Halfway from green to yellow: red channel at 127, green stays 255.
        assertEquals(0xFF7FFF00, F3Format.barColor(25, 100));
    }

    @Test
    void lerpColor_endpoints() {
        assertEquals(0xFF000000, F3Format.lerpColor(0xFF000000, 0xFFFFFFFF, 0.0F));
        assertEquals(0xFFFFFFFF, F3Format.lerpColor(0xFF000000, 0xFFFFFFFF, 1.0F));
    }

    @Test
    void stepRenderDistance_stepsAndClamps() {
        assertEquals(13, F3Format.stepRenderDistance(12, false, 2, 32));
        assertEquals(11, F3Format.stepRenderDistance(12, true, 2, 32));
        assertEquals(32, F3Format.stepRenderDistance(32, false, 2, 32));
        assertEquals(2, F3Format.stepRenderDistance(2, true, 2, 32));
    }

    @Test
    void particleTotal_keepsOnlyTheTotal() {
        assertEquals("16", F3Format.particleTotal("SO 16 T 16"));
        assertEquals("40", F3Format.particleTotal("SO 12 TR 28 T 40 "));
        assertEquals("0", F3Format.particleTotal("T 0"));
    }

    @Test
    void particleTotal_fallsBackToTheRawString() {
        assertEquals("unknown", F3Format.particleTotal("unknown"));
        assertEquals("", F3Format.particleTotal(""));
    }

    @Test
    void ticksPerSecond_fromMillisPerTick() {
        assertEquals(20, F3Format.ticksPerSecond(50.0F));
        assertEquals(40, F3Format.ticksPerSecond(25.0F));
        assertEquals(0, F3Format.ticksPerSecond(0.0F));
    }
}
