package de.hasil.pictree.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Reine Geometrie-Helfer für die Vorschau: Bild in Panel einpassen
 * (Letterbox) und Umrechnung zwischen Panel-Pixeln und relativen
 * Bildkoordinaten (0..1). Bewusst ohne Swing-Abhängigkeit, damit voll testbar.
 */
public final class PreviewGeometry {

    private PreviewGeometry() {
    }

    /**
     * Berechnet das seitenverhältnistreue Zielrechteck, in das ein Bild
     * ({@code iw}×{@code ih}) innerhalb einer Fläche ({@code pw}×{@code ph})
     * zentriert eingepasst wird.
     */
    public static Rectangle computeFitRect(int iw, int ih, int pw, int ph) {
        if (iw <= 0 || ih <= 0 || pw <= 0 || ph <= 0) {
            return new Rectangle(0, 0, 0, 0);
        }
        double scale = Math.min((double) pw / iw, (double) ph / ih);
        int w = Math.max(1, (int) Math.round(iw * scale));
        int h = Math.max(1, (int) Math.round(ih * scale));
        int x = (pw - w) / 2;
        int y = (ph - h) / 2;
        return new Rectangle(x, y, w, h);
    }

    /**
     * Wandelt einen Panel-Punkt in relative Bildkoordinaten (0..1) um,
     * bezogen auf das Einpass-Rechteck. Werte außerhalb werden auf [0,1] geklemmt.
     */
    public static Point2D.Double panelToRelative(Point p, Rectangle fit) {
        if (fit.width <= 0 || fit.height <= 0) {
            return new Point2D.Double(0.5, 0.5);
        }
        double rx = (double) (p.x - fit.x) / fit.width;
        double ry = (double) (p.y - fit.y) / fit.height;
        return new Point2D.Double(clamp01(rx), clamp01(ry));
    }

    /** Wandelt relative Bildkoordinaten (0..1) in einen Panel-Punkt innerhalb des Einpass-Rechtecks. */
    public static Point relativeToPanel(double relX, double relY, Rectangle fit) {
        int x = fit.x + (int) Math.round(clamp01(relX) * fit.width);
        int y = fit.y + (int) Math.round(clamp01(relY) * fit.height);
        return new Point(x, y);
    }

    private static double clamp01(double v) {
        if (v < 0) {
            return 0;
        }
        if (v > 1) {
            return 1;
        }
        return v;
    }
}
