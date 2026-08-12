package de.hasil.pictree.ui;

/**
 * Rastet relative Koordinaten (0..1) an sinnvolle Ziele ein: Ränder, Mitte und
 * Drittel. Rein rechnerisch, damit voll testbar.
 */
public final class Snapping {

    /** Einrast-Ziele: 0, 1/3, Mitte, 2/3, 1. */
    public static final double[] TARGETS = {0.0, 1.0 / 3.0, 0.5, 2.0 / 3.0, 1.0};

    /** Standard-Toleranz (in relativen Einheiten). */
    public static final double DEFAULT_THRESHOLD = 0.02;

    private Snapping() {
    }

    /** Ergebnis eines Snap-Versuchs. */
    public record Snap(double value, boolean snapped) {
    }

    /** Rastet {@code value} an das nächste Ziel innerhalb von {@code threshold} ein. */
    public static Snap snap(double value, double threshold) {
        double best = value;
        double bestDist = threshold;
        boolean snapped = false;
        for (double target : TARGETS) {
            double dist = Math.abs(value - target);
            if (dist <= bestDist) {
                bestDist = dist;
                best = target;
                snapped = true;
            }
        }
        return new Snap(best, snapped);
    }

    public static Snap snap(double value) {
        return snap(value, DEFAULT_THRESHOLD);
    }
}
