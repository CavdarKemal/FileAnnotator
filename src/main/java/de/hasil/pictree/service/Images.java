package de.hasil.pictree.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Kleine Bild-Hilfsfunktionen.
 */
public final class Images {

    private Images() {
    }

    /**
     * Skaliert ein Bild so herunter, dass seine längere Kante höchstens
     * {@code maxDimension} Pixel misst. Ist das Bild bereits klein genug, wird
     * dieselbe Instanz zurückgegeben (kein unnötiges Kopieren).
     */
    public static BufferedImage downscaleToMax(BufferedImage src, int maxDimension) {
        if (src == null || maxDimension <= 0) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int longer = Math.max(w, h);
        if (longer <= maxDimension) {
            return src;
        }
        double scale = (double) maxDimension / longer;
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));

        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return dst;
    }
}
