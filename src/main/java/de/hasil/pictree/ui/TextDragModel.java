package de.hasil.pictree.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Kapselt die Zieh-Logik für den Text-Stempel: Treffer-Test auf den Textblock
 * und Umrechnung einer Mausbewegung in eine neue relative Ankerposition (0..1).
 * Bewusst Swing-frei und damit voll unit-testbar.
 */
public class TextDragModel {

    private int offsetX;
    private int offsetY;
    private boolean dragging;

    public boolean isDragging() {
        return dragging;
    }

    /**
     * Prüft, ob {@code p} (mit Toleranz {@code pad}) innerhalb des Textrechtecks liegt.
     */
    public static boolean hitTest(Point p, Rectangle textRect, int pad) {
        if (textRect == null || p == null) {
            return false;
        }
        Rectangle padded = new Rectangle(
                textRect.x - pad, textRect.y - pad,
                textRect.width + 2 * pad, textRect.height + 2 * pad);
        return padded.contains(p);
    }

    /**
     * Startet das Ziehen: merkt sich den Versatz zwischen Mauspunkt und dem
     * aktuellen Anker (Textmittelpunkt) in Panel-Koordinaten.
     */
    public void begin(Point p, double relX, double relY, Rectangle fit) {
        Point anchor = PreviewGeometry.relativeToPanel(relX, relY, fit);
        offsetX = p.x - anchor.x;
        offsetY = p.y - anchor.y;
        dragging = true;
    }

    /**
     * Liefert die neue relative Ankerposition (0..1) für den aktuellen Mauspunkt,
     * unter Berücksichtigung des beim {@link #begin} gemerkten Versatzes.
     */
    public Point2D.Double update(Point p, Rectangle fit) {
        Point shifted = new Point(p.x - offsetX, p.y - offsetY);
        return PreviewGeometry.panelToRelative(shifted, fit);
    }

    public void end() {
        dragging = false;
    }
}
