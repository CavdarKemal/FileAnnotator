package de.hasil.pictree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SnappingTest {

    @Test
    void snapsToCenterWhenClose() {
        Snapping.Snap s = Snapping.snap(0.49);
        assertTrue(s.snapped());
        assertEquals(0.5, s.value(), 1e-9);
    }

    @Test
    void snapsToThirds() {
        Snapping.Snap s = Snapping.snap(0.66);
        assertTrue(s.snapped());
        assertEquals(2.0 / 3.0, s.value(), 1e-9);
    }

    @Test
    void snapsToEdges() {
        assertEquals(0.0, Snapping.snap(0.01).value(), 1e-9);
        assertEquals(1.0, Snapping.snap(0.995).value(), 1e-9);
    }

    @Test
    void doesNotSnapWhenFarFromTargets() {
        Snapping.Snap s = Snapping.snap(0.42);
        assertFalse(s.snapped());
        assertEquals(0.42, s.value(), 1e-9);
    }

    @Test
    void picksNearestTargetWithinThreshold() {
        // 0.35 liegt näher an 1/3 (0.3333) als an 0.5; großzügige Toleranz.
        Snapping.Snap s = Snapping.snap(0.35, 0.2);
        assertEquals(1.0 / 3.0, s.value(), 1e-9);
    }
}
