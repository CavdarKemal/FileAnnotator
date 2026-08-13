package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import de.hasil.pictree.model.StampStyle;

class TextStampRendererTest {

    private BufferedImage blackCanvas(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private long nonBlackPixels(BufferedImage img) {
        long count = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void drawingTextChangesPixelsAndReturnsBounds() {
        BufferedImage img = blackCanvas(300, 200);
        Graphics2D g = img.createGraphics();
        StampStyle style = new StampStyle();
        style.setColor(Color.WHITE);

        Rectangle bounds = TextStampRenderer.drawStamp(g, "Hallo", style, new Rectangle(0, 0, 300, 200));
        g.dispose();

        assertNotNull(bounds);
        assertTrue(bounds.width > 0 && bounds.height > 0);
        assertTrue(nonBlackPixels(img) > 0, "Text muss Pixel verändert haben");
    }

    @Test
    void blankTextDrawsNothing() {
        BufferedImage img = blackCanvas(100, 100);
        Graphics2D g = img.createGraphics();
        Rectangle bounds = TextStampRenderer.drawStamp(g, "   ", new StampStyle(), new Rectangle(0, 0, 100, 100));
        g.dispose();
        assertNull(bounds);
        assertTrue(nonBlackPixels(img) == 0);
    }

    @Test
    void largerRelativeSizeProducesLargerBounds() {
        BufferedImage img = blackCanvas(400, 400);
        Graphics2D g = img.createGraphics();
        Rectangle area = new Rectangle(0, 0, 400, 400);

        StampStyle small = new StampStyle();
        small.setRelativeSize(0.05f);
        Rectangle smallBounds = TextStampRenderer.drawStamp(g, "X", small, area);

        StampStyle big = new StampStyle();
        big.setRelativeSize(0.20f);
        Rectangle bigBounds = TextStampRenderer.drawStamp(g, "X", big, area);
        g.dispose();

        assertTrue(bigBounds.height > smallBounds.height);
    }
}
