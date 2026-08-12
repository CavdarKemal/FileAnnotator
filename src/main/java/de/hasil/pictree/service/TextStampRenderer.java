package de.hasil.pictree.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import de.hasil.pictree.model.StampStyle;

/**
 * Zeichnet einen Text-Stempel in eine gegebene Fläche. Wird sowohl von der
 * Vorschau (Fläche = eingepasstes Bildrechteck in Panel-Pixeln) als auch vom
 * Speicher-Service (Fläche = volle Bildauflösung) verwendet – identische Logik
 * garantiert WYSIWYG.
 */
public final class TextStampRenderer {

    private TextStampRenderer() {
    }

    /**
     * Zeichnet {@code text} gemäß {@code style} in {@code area}.
     *
     * @return die Bounding-Box des gezeichneten Textblocks in Koordinaten von
     *         {@code area}, oder {@code null} bei leerem Text.
     */
    public static Rectangle drawStamp(Graphics2D g, String text, StampStyle style, Rectangle area) {
        if (text == null || text.isBlank() || area.width <= 0 || area.height <= 0) {
            return null;
        }

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int fontSize = Math.max(1, Math.round(style.getRelativeSize() * area.height));
        Font font = FontRegistry.resolve(style.getFontFamily(), style.getFontStyle(), fontSize);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        String[] lines = text.split("\n", -1);
        int lineHeight = fm.getHeight();
        int blockHeight = lineHeight * lines.length;
        int blockWidth = 0;
        for (String line : lines) {
            blockWidth = Math.max(blockWidth, fm.stringWidth(line));
        }
        if (blockWidth <= 0) {
            blockWidth = 1;
        }

        int cx = area.x + (int) Math.round(clamp01(style.getRelX()) * area.width);
        int cy = area.y + (int) Math.round(clamp01(style.getRelY()) * area.height);
        int x0 = cx - blockWidth / 2;
        int y0 = cy - blockHeight / 2;

        int outlinePx = Math.max(1, Math.round(fontSize * 0.06f));
        int baseline = y0 + fm.getAscent();
        for (String line : lines) {
            int lineWidth = fm.stringWidth(line);
            int lineX = x0 + (blockWidth - lineWidth) / 2;
            if (style.isOutline()) {
                g.setColor(contrastColor(style.getColor()));
                for (int dx = -outlinePx; dx <= outlinePx; dx++) {
                    for (int dy = -outlinePx; dy <= outlinePx; dy++) {
                        if (dx != 0 || dy != 0) {
                            g.drawString(line, lineX + dx, baseline + dy);
                        }
                    }
                }
            }
            g.setColor(style.getColor());
            g.drawString(line, lineX, baseline);
            baseline += lineHeight;
        }

        return new Rectangle(x0, y0, blockWidth, blockHeight);
    }

    /** Dunkler Umriss für helle Schrift, heller Umriss für dunkle Schrift. */
    private static Color contrastColor(Color text) {
        double luminance = (0.299 * text.getRed() + 0.587 * text.getGreen() + 0.114 * text.getBlue()) / 255.0;
        return luminance < 0.5 ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 200);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
