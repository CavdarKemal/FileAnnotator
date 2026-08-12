package de.hasil.pictree.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import de.hasil.pictree.model.LogoOverlay;
import de.hasil.pictree.model.StampStyle;

/**
 * Rendert einen Text-Stempel (und optional ein Logo-Overlay) auf eine
 * <b>Kopie</b> des Bildes in voller Auflösung. Das Originalbild wird nie
 * verändert. Verwendet dieselbe {@link TextStampRenderer}-Logik wie die
 * Vorschau (WYSIWYG).
 */
public final class ImageStampService {

    private ImageStampService() {
    }

    /** Ohne Logo-Overlay. */
    public static BufferedImage renderStamp(BufferedImage source, String text, StampStyle style) {
        return renderStamp(source, text, style, null);
    }

    /**
     * Erzeugt eine neue, mit Logo und Text versehene RGB-Kopie des Quellbildes.
     *
     * @param source Quellbild (unverändert)
     * @param text   Stempeltext (leer/null => kein Text)
     * @param style  Stil und relative Position
     * @param logo   optionales Logo-Overlay (darf {@code null} sein)
     * @return neue {@link BufferedImage}-Instanz (TYPE_INT_RGB)
     */
    public static BufferedImage renderStamp(BufferedImage source, String text, StampStyle style,
            LogoOverlay logo) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // Weißer Untergrund für den Fall, dass die Quelle Transparenz besitzt (z. B. PNG).
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.drawImage(source, 0, 0, null);
            Rectangle area = new Rectangle(0, 0, w, h);
            LogoOverlayRenderer.draw(g, logo, area);
            TextStampRenderer.drawStamp(g, text, style, area);
        } finally {
            g.dispose();
        }
        return out;
    }
}
