package de.hasil.pictree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class PreviewGeometryTest {

    @Test
    void fitRectIsLetterboxedAndCentered() {
        // 200x100 Bild in 400x400 Panel -> Breite limitiert: 400x200, zentriert y=100.
        Rectangle r = PreviewGeometry.computeFitRect(200, 100, 400, 400);
        assertEquals(400, r.width);
        assertEquals(200, r.height);
        assertEquals(0, r.x);
        assertEquals(100, r.y);
    }

    @Test
    void fitRectHandlesTallImage() {
        // 100x200 Bild in 400x400 -> Höhe limitiert: 200x400, zentriert x=100.
        Rectangle r = PreviewGeometry.computeFitRect(100, 200, 400, 400);
        assertEquals(200, r.width);
        assertEquals(400, r.height);
        assertEquals(100, r.x);
        assertEquals(0, r.y);
    }

    @Test
    void degenerateInputsReturnEmpty() {
        assertEquals(new Rectangle(0, 0, 0, 0), PreviewGeometry.computeFitRect(0, 100, 400, 400));
        assertEquals(new Rectangle(0, 0, 0, 0), PreviewGeometry.computeFitRect(100, 100, 0, 400));
    }

    @Test
    void panelToRelativeAndBackAreConsistent() {
        Rectangle fit = new Rectangle(50, 20, 200, 100);
        // Mittelpunkt des Bildrechtecks -> (0.5, 0.5).
        Point2D.Double rel = PreviewGeometry.panelToRelative(new Point(150, 70), fit);
        assertEquals(0.5, rel.x, 1e-9);
        assertEquals(0.5, rel.y, 1e-9);

        Point back = PreviewGeometry.relativeToPanel(0.5, 0.5, fit);
        assertEquals(150, back.x);
        assertEquals(70, back.y);
    }

    @Test
    void panelToRelativeClampsOutsidePoints() {
        Rectangle fit = new Rectangle(0, 0, 100, 100);
        Point2D.Double rel = PreviewGeometry.panelToRelative(new Point(-30, 500), fit);
        assertTrue(rel.x >= 0.0 && rel.x <= 1.0);
        assertTrue(rel.y >= 0.0 && rel.y <= 1.0);
        assertEquals(0.0, rel.x, 1e-9);
        assertEquals(1.0, rel.y, 1e-9);
    }

    @Test
    void zoomedRectAtZoomOneEqualsBase() {
        Rectangle base = new Rectangle(10, 20, 100, 80);
        assertEquals(base, PreviewGeometry.zoomedRect(base, 1.0, 0, 0));
    }

    @Test
    void zoomedRectScalesAndCenters() {
        Rectangle base = new Rectangle(0, 0, 100, 100);
        Rectangle z = PreviewGeometry.zoomedRect(base, 2.0, 0, 0);
        assertEquals(200, z.width);
        assertEquals(200, z.height);
        assertEquals(-50, z.x); // zentriert: (100-200)/2
        assertEquals(-50, z.y);
    }

    @Test
    void panForZoomAboutKeepsCursorPointStable() {
        Rectangle base = new Rectangle(0, 0, 100, 100);
        // Cursor links oben (fx=fy=0), Zoom 2x: der Punkt soll unter (0,0) bleiben.
        int[] pan = PreviewGeometry.panForZoomAbout(base, 2.0, 0, 0, 0.0, 0.0);
        Rectangle z = PreviewGeometry.zoomedRect(base, 2.0, pan[0], pan[1]);
        // Bildpunkt (fx=0) liegt bei z.x -> muss wieder 0 sein.
        assertEquals(0, z.x);
        assertEquals(0, z.y);
    }

    @Test
    void panForZoomAboutCenterNeedsNoPan() {
        Rectangle base = new Rectangle(0, 0, 100, 100);
        int[] pan = PreviewGeometry.panForZoomAbout(base, 2.0, 50, 50, 0.5, 0.5);
        assertEquals(0, pan[0]);
        assertEquals(0, pan[1]);
    }

    @Test
    void insetRectShrinksSymmetrically() {
        Rectangle r = PreviewGeometry.insetRect(new Rectangle(0, 0, 200, 100), 0.05);
        assertEquals(10, r.x);   // 5% von 200
        assertEquals(5, r.y);    // 5% von 100
        assertEquals(180, r.width);
        assertEquals(90, r.height);
    }
}
