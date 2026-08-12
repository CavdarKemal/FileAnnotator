package de.hasil.pictree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.awt.Color;
import java.awt.Font;

import org.junit.jupiter.api.Test;

class StampStyleTest {

    @Test
    void relativePositionIsClampedTo01() {
        StampStyle s = new StampStyle();
        s.setRelX(1.7);
        s.setRelY(-0.4);
        assertEquals(1.0, s.getRelX(), 1e-9);
        assertEquals(0.0, s.getRelY(), 1e-9);
    }

    @Test
    void copyIsIndependentDeepCopy() {
        StampStyle s = new StampStyle();
        s.setColor(Color.RED);
        s.setFontFamily("Roboto");
        s.setFontStyle(Font.BOLD);
        s.setRelativeSize(0.12f);
        s.setRelX(0.3);

        StampStyle c = s.copy();
        assertNotSame(s, c);
        assertEquals(Color.RED, c.getColor());
        assertEquals("Roboto", c.getFontFamily());
        assertEquals(Font.BOLD, c.getFontStyle());
        assertEquals(0.12f, c.getRelativeSize(), 1e-6);
        assertEquals(0.3, c.getRelX(), 1e-9);

        // Änderung an der Kopie darf das Original nicht berühren.
        c.setColor(Color.BLUE);
        assertEquals(Color.RED, s.getColor());
    }
}
