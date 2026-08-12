package de.hasil.pictree.model;

import java.awt.image.BufferedImage;

/**
 * Bündelt alle Eingaben für das Rendern eines bestempelten Bildes: Quellbild,
 * Text, Stil und optionales Logo-Overlay. Ersetzt lange Parameterketten.
 */
public record RenderRequest(BufferedImage source, String text, StampStyle style, LogoOverlay logo) {

    /** Bequemer Konstruktor ohne Logo. */
    public static RenderRequest of(BufferedImage source, String text, StampStyle style) {
        return new RenderRequest(source, text, style, null);
    }
}
