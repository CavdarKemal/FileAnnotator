package de.hasil.pictree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class TextDragModelTest {

    @Test
    void hitTestRespectsPadding() {
        Rectangle text = new Rectangle(100, 100, 40, 20);
        assertTrue(TextDragModel.hitTest(new Point(120, 110), text, 0));   // mittendrin
        assertFalse(TextDragModel.hitTest(new Point(95, 110), text, 0));   // knapp links außerhalb
        assertTrue(TextDragModel.hitTest(new Point(95, 110), text, 8));    // mit Toleranz drin
        assertFalse(TextDragModel.hitTest(new Point(0, 0), null, 8));      // kein Textrechteck
    }

    @Test
    void draggingByDeltaMovesAnchorAccordingly() {
        Rectangle fit = new Rectangle(0, 0, 200, 100);
        TextDragModel drag = new TextDragModel();

        // Anker liegt bei rel (0.5, 0.5) => Panelpunkt (100, 50).
        // Der Nutzer greift exakt am Anker und zieht 40px nach rechts, 10px nach unten.
        Point grab = new Point(100, 50);
        drag.begin(grab, 0.5, 0.5, fit);
        assertTrue(drag.isDragging());

        Point2D.Double rel = drag.update(new Point(140, 60), fit);
        assertEquals(0.7, rel.x, 1e-9);   // (140-0)/200
        assertEquals(0.6, rel.y, 1e-9);   // (60-0)/100

        drag.end();
        assertFalse(drag.isDragging());
    }

    @Test
    void grabOffsetIsPreserved() {
        Rectangle fit = new Rectangle(0, 0, 100, 100);
        TextDragModel drag = new TextDragModel();

        // Anker bei rel (0.5,0.5) => (50,50). Nutzer greift versetzt bei (60,50).
        drag.begin(new Point(60, 50), 0.5, 0.5, fit);
        // Zieht Maus auf (70,50): Anker soll sich um denselben Betrag (10) verschieben => (60,50) => rel 0.6.
        Point2D.Double rel = drag.update(new Point(70, 50), fit);
        assertEquals(0.6, rel.x, 1e-9);
        assertEquals(0.5, rel.y, 1e-9);
    }

    @Test
    void draggingOutsideClampsToBounds() {
        Rectangle fit = new Rectangle(0, 0, 100, 100);
        TextDragModel drag = new TextDragModel();
        drag.begin(new Point(50, 50), 0.5, 0.5, fit);
        Point2D.Double rel = drag.update(new Point(500, -20), fit);
        assertEquals(1.0, rel.x, 1e-9);
        assertEquals(0.0, rel.y, 1e-9);
    }
}
