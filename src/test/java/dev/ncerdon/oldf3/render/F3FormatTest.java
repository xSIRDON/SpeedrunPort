package dev.ncerdon.oldf3.render;

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
}
