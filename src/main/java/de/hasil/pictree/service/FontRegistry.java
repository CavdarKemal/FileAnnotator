package de.hasil.pictree.service;

import java.awt.Font;

/**
 * Zentrale Font-Auflösung. In Schritt 4 wird hier zusätzlich die gebündelte
 * Roboto-Schrift registriert und ausgeliefert. Vorerst reine System-Fonts.
 */
public final class FontRegistry {

    private FontRegistry() {
    }

    /** Liefert einen Font für die gegebene Familie/Stil/Größe. */
    public static Font resolve(String family, int style, int size) {
        return new Font(family, style, Math.max(1, size));
    }
}
