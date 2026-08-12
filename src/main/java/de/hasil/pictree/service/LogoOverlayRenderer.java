package de.hasil.pictree.service;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import de.hasil.pictree.model.LogoOverlay;

/**
 * Zeichnet ein Logo-/Wasserzeichen-Overlay mit Deckkraft in eine Fläche.
 * Wird von Vorschau und Speichern gemeinsam genutzt (WYSIWYG).
 */
public final class LogoOverlayRenderer {

    private LogoOverlayRenderer() {
    }

    /** Zeichnet das Overlay skaliert/positioniert mit Deckkraft in {@code area}. */
    public static void draw(Graphics2D g, LogoOverlay overlay, Rectangle area) {
        if (overlay == null || overlay.getImage() == null || area.width <= 0 || area.height <= 0) {
            return;
        }
        BufferedImage logo = overlay.getImage();
        int w = (int) Math.round(overlay.getRelWidthFraction() * area.width);
        if (w <= 0 || logo.getWidth() <= 0) {
            return;
        }
        int h = (int) Math.round(w * (double) logo.getHeight() / logo.getWidth());
        int cx = area.x + (int) Math.round(overlay.getRelX() * area.width);
        int cy = area.y + (int) Math.round(overlay.getRelY() * area.height);
        int x = cx - w / 2;
        int y = cy - h / 2;

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlay.getOpacity()));
        g.drawImage(logo, x, y, w, h, null);
        g.setComposite(old);
    }
}
