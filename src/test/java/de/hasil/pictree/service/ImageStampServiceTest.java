package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import de.hasil.pictree.model.StampStyle;

class ImageStampServiceTest {

    private BufferedImage solid(int w, int h, Color c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private long snapshotHash(BufferedImage img) {
        long h = 1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                h = h * 31 + img.getRGB(x, y);
            }
        }
        return h;
    }

    @Test
    void renderProducesNewImageAndLeavesSourceUnchanged() {
        BufferedImage src = solid(120, 90, Color.BLACK);
        long before = snapshotHash(src);

        StampStyle style = new StampStyle();
        style.setColor(Color.WHITE);
        BufferedImage out = ImageStampService.renderStamp(src, "Test", style);

        assertNotSame(src, out);
        assertEquals(120, out.getWidth());
        assertEquals(90, out.getHeight());
        // Quelle unverändert:
        assertEquals(before, snapshotHash(src));
    }

    @Test
    void stampChangesPixelsRelativeToSource() {
        BufferedImage src = solid(200, 150, Color.BLACK);
        StampStyle style = new StampStyle();
        style.setColor(Color.WHITE);
        BufferedImage out = ImageStampService.renderStamp(src, "ABC", style);

        int white = Color.WHITE.getRGB() & 0xFFFFFF;
        long whitePixels = 0;
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                if ((out.getRGB(x, y) & 0xFFFFFF) == white) {
                    whitePixels++;
                }
            }
        }
        assertTrue(whitePixels > 0, "Der weiße Stempeltext muss sichtbar sein");
    }
}
