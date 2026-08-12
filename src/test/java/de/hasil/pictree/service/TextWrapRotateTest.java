package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.hasil.pictree.model.StampStyle;

class TextWrapRotateTest {

    private FontMetrics metrics() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setFont(FontRegistry.resolve("SansSerif", java.awt.Font.PLAIN, 20));
        return g.getFontMetrics();
    }

    @Test
    void noWrapKeepsExplicitLines() {
        List<String> lines = TextStampRenderer.wrapLines(metrics(), "a\nb\nc", 0);
        assertEquals(List.of("a", "b", "c"), lines);
    }

    @Test
    void wrapsLongParagraphIntoMultipleLines() {
        FontMetrics fm = metrics();
        String text = "Dies ist ein ziemlich langer Satz der umgebrochen werden soll";
        int maxWidth = fm.stringWidth("Dies ist ein"); // sehr schmal
        List<String> lines = TextStampRenderer.wrapLines(fm, text, maxWidth);
        assertTrue(lines.size() > 1, "Text sollte umgebrochen werden");
        for (String line : lines) {
            // Jede Zeile (außer evtl. Einzelwörter) passt grob in die Breite.
            assertTrue(fm.stringWidth(line) <= maxWidth || !line.contains(" "), line);
        }
    }

    @Test
    void longSingleWordIsNotSplit() {
        FontMetrics fm = metrics();
        List<String> lines = TextStampRenderer.wrapLines(fm, "Donaudampfschifffahrtsgesellschaft", 5);
        assertEquals(1, lines.size());
    }

    @Test
    void rotationEnlargesAxisAlignedBounds() {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Rectangle area = new Rectangle(0, 0, 400, 400);

        StampStyle straight = new StampStyle();
        Rectangle b0 = TextStampRenderer.drawStamp(g, "Text", straight, area);

        StampStyle rotated = new StampStyle();
        rotated.setRotationDegrees(45);
        Rectangle b45 = TextStampRenderer.drawStamp(g, "Text", rotated, area);
        g.dispose();

        assertNotNull(b0);
        assertNotNull(b45);
        // 45°-Drehung vergrößert die achsparallele Hüllbox.
        assertTrue(b45.height > b0.height, "Rotation sollte die Hoehe der Huellbox vergroessern");
    }

    @Test
    void wrappedTextRendersPixels() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 200, 200);
        StampStyle s = new StampStyle();
        s.setColor(Color.WHITE);
        s.setWrapWidthFraction(0.5);
        Rectangle b = TextStampRenderer.drawStamp(g, "eins zwei drei vier fuenf sechs", s,
                new Rectangle(0, 0, 200, 200));
        g.dispose();
        assertNotNull(b);
        assertTrue(b.height > 0);
    }
}
