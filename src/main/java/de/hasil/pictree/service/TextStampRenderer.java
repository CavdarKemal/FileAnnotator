package de.hasil.pictree.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import de.hasil.pictree.model.StampStyle;

/**
 * Zeichnet einen Text-Stempel in eine gegebene Fläche. Wird sowohl von der
 * Vorschau (Fläche = eingepasstes Bildrechteck in Panel-Pixeln) als auch vom
 * Speicher-Service (Fläche = volle Bildauflösung) verwendet – identische Logik
 * garantiert WYSIWYG. Unterstützt automatischen Zeilenumbruch und Rotation.
 */
public final class TextStampRenderer {

    private TextStampRenderer() {
    }

    /**
     * Zeichnet {@code text} gemäß {@code style} in {@code area}.
     *
     * @return die (achsparallele) Bounding-Box des Textblocks in Koordinaten von
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

        int maxWidth = style.getWrapWidthFraction() > 0
                ? (int) Math.round(style.getWrapWidthFraction() * area.width)
                : 0;
        List<String> lines = wrapLines(fm, text, maxWidth);

        int lineHeight = fm.getHeight();
        int blockHeight = lineHeight * lines.size();
        int blockWidth = 1;
        for (String line : lines) {
            blockWidth = Math.max(blockWidth, fm.stringWidth(line));
        }

        int cx = area.x + (int) Math.round(clamp01(style.getRelX()) * area.width);
        int cy = area.y + (int) Math.round(clamp01(style.getRelY()) * area.height);
        double angle = Math.toRadians(style.getRotationDegrees());

        AffineTransform saved = g.getTransform();
        g.translate(cx, cy);
        if (angle != 0) {
            g.rotate(angle);
        }

        int x0 = -blockWidth / 2;
        int y0 = -blockHeight / 2;
        int outlinePx = Math.max(1, Math.round(fontSize * 0.06f));

        // Halbtransparente Box/Banner hinter dem Text.
        if (style.isBoxEnabled()) {
            int pad = Math.max(2, Math.round(fontSize * 0.25f));
            Color bc = style.getBoxColor();
            int alpha = Math.round(style.getBoxOpacity() * 255);
            g.setColor(new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), alpha));
            g.fillRoundRect(x0 - pad, y0 - pad, blockWidth + 2 * pad, blockHeight + 2 * pad,
                    pad, pad);
        }

        int shadowOffset = style.getShadowStrength() > 0
                ? Math.max(1, Math.round(fontSize * 0.10f * style.getShadowStrength()))
                : 0;
        int shadowAlpha = Math.round(200 * style.getShadowStrength());

        int baseline = y0 + fm.getAscent();
        for (String line : lines) {
            int lineWidth = fm.stringWidth(line);
            int lineX = x0 + (blockWidth - lineWidth) / 2;
            if (shadowOffset > 0 && !line.isEmpty()) {
                g.setColor(new Color(0, 0, 0, shadowAlpha));
                g.drawString(line, lineX + shadowOffset, baseline + shadowOffset);
            }
            if (style.isOutline() && !line.isEmpty()) {
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
        g.setTransform(saved);

        return rotatedBounds(cx, cy, blockWidth, blockHeight, angle);
    }

    /**
     * Bricht Text auf {@code maxWidth} Pixel um (0 = kein Umbruch). Explizite
     * Zeilenumbrüche bleiben erhalten; einzelne überlange Wörter werden nicht
     * getrennt.
     */
    static List<String> wrapLines(FontMetrics fm, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (maxWidth <= 0 || paragraph.isEmpty()) {
                out.add(paragraph);
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (current.length() == 0 || fm.stringWidth(candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    out.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            out.add(current.toString());
        }
        return out;
    }

    /** Achsparallele Bounding-Box eines um {@code angle} gedrehten Blocks um (cx,cy). */
    private static Rectangle rotatedBounds(int cx, int cy, int w, int h, double angle) {
        if (angle == 0) {
            return new Rectangle(cx - w / 2, cy - h / 2, w, h);
        }
        double cos = Math.abs(Math.cos(angle));
        double sin = Math.abs(Math.sin(angle));
        int bw = (int) Math.ceil(w * cos + h * sin);
        int bh = (int) Math.ceil(w * sin + h * cos);
        return new Rectangle(cx - bw / 2, cy - bh / 2, bw, bh);
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
