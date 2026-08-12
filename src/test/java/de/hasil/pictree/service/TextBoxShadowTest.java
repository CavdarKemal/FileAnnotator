package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import de.hasil.pictree.model.StampStyle;

class TextBoxShadowTest {

    private BufferedImage white(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private long nonWhitePixels(BufferedImage img) {
        long count = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void boxAddsBackgroundFillBehindText() {
        Rectangle area = new Rectangle(0, 0, 300, 200);

        BufferedImage noBox = white(300, 200);
        Graphics2D g1 = noBox.createGraphics();
        StampStyle plain = new StampStyle();
        plain.setColor(Color.WHITE); // Text unsichtbar auf Weiß -> nur Umriss/Box zählt
        plain.setOutline(false);
        TextStampRenderer.drawStamp(g1, "Hallo", plain, area);
        g1.dispose();

        BufferedImage withBox = white(300, 200);
        Graphics2D g2 = withBox.createGraphics();
        StampStyle boxed = new StampStyle();
        boxed.setColor(Color.WHITE);
        boxed.setOutline(false);
        boxed.setBoxEnabled(true);
        boxed.setBoxColor(Color.BLACK);
        boxed.setBoxOpacity(1.0f);
        TextStampRenderer.drawStamp(g2, "Hallo", boxed, area);
        g2.dispose();

        assertTrue(nonWhitePixels(withBox) > nonWhitePixels(noBox),
                "Die Box sollte zusätzliche gefüllte Pixel erzeugen");
    }

    @Test
    void shadowAddsDarkPixels() {
        Rectangle area = new Rectangle(0, 0, 300, 200);

        BufferedImage noShadow = white(300, 200);
        Graphics2D g1 = noShadow.createGraphics();
        StampStyle plain = new StampStyle();
        plain.setColor(Color.WHITE);
        plain.setOutline(false);
        TextStampRenderer.drawStamp(g1, "Hallo", plain, area);
        g1.dispose();

        BufferedImage withShadow = white(300, 200);
        Graphics2D g2 = withShadow.createGraphics();
        StampStyle shadowed = new StampStyle();
        shadowed.setColor(Color.WHITE);
        shadowed.setOutline(false);
        shadowed.setShadowStrength(1.0f);
        TextStampRenderer.drawStamp(g2, "Hallo", shadowed, area);
        g2.dispose();

        assertTrue(nonWhitePixels(withShadow) > nonWhitePixels(noShadow),
                "Der Schatten sollte dunkle Pixel hinzufügen");
    }
}
