package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import de.hasil.pictree.model.LogoOverlay;

class LogoOverlayRendererTest {

    private BufferedImage solid(int w, int h, Color c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    @Test
    void drawsLogoOpaqueAtCenter() {
        BufferedImage canvas = solid(200, 200, Color.BLACK);
        BufferedImage logo = solid(50, 50, Color.RED);
        LogoOverlay overlay = new LogoOverlay(null, logo);
        overlay.setRelX(0.5);
        overlay.setRelY(0.5);
        overlay.setRelWidthFraction(0.25); // 50 px
        overlay.setOpacity(1.0f);

        Graphics2D g = canvas.createGraphics();
        LogoOverlayRenderer.draw(g, overlay, new Rectangle(0, 0, 200, 200));
        g.dispose();

        // Zentrum muss rot sein.
        assertEquals(Color.RED.getRGB() & 0xFFFFFF, canvas.getRGB(100, 100) & 0xFFFFFF);
        // Ecke bleibt schwarz.
        assertEquals(0, canvas.getRGB(0, 0) & 0xFFFFFF);
    }

    @Test
    void opacityBlendsWithBackground() {
        BufferedImage canvas = solid(100, 100, Color.BLACK);
        BufferedImage logo = solid(100, 100, Color.WHITE);
        LogoOverlay overlay = new LogoOverlay(null, logo);
        overlay.setRelX(0.5);
        overlay.setRelY(0.5);
        overlay.setRelWidthFraction(1.0);
        overlay.setOpacity(0.5f);

        Graphics2D g = canvas.createGraphics();
        LogoOverlayRenderer.draw(g, overlay, new Rectangle(0, 0, 100, 100));
        g.dispose();

        int gray = canvas.getRGB(50, 50) & 0xFF; // Blau-Kanal als Graustufe
        // 50 % Weiß auf Schwarz -> etwa mittelgrau.
        assertTrue(gray > 90 && gray < 165, "erwartet Mittelgrau, war " + gray);
    }

    @Test
    void nullOverlayIsNoOp() {
        BufferedImage canvas = solid(20, 20, Color.BLACK);
        Graphics2D g = canvas.createGraphics();
        LogoOverlayRenderer.draw(g, null, new Rectangle(0, 0, 20, 20));
        g.dispose();
        assertEquals(0, canvas.getRGB(10, 10) & 0xFFFFFF);
    }
}
