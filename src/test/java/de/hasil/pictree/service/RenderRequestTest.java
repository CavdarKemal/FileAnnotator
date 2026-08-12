package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import de.hasil.pictree.model.RenderRequest;
import de.hasil.pictree.model.StampStyle;

class RenderRequestTest {

    private BufferedImage solid(int w, int h, Color c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private long hash(BufferedImage img) {
        long h = 1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                h = h * 31 + img.getRGB(x, y);
            }
        }
        return h;
    }

    @Test
    void renderRequestMatchesLegacyOverload() {
        BufferedImage src = solid(120, 90, Color.BLACK);
        StampStyle style = new StampStyle();
        style.setColor(Color.WHITE);

        BufferedImage viaOverload = ImageStampService.renderStamp(src, "Test", style);
        BufferedImage viaRequest = ImageStampService.render(RenderRequest.of(src, "Test", style));

        assertEquals(hash(viaOverload), hash(viaRequest));
    }

    @Test
    void ofFactorySetsNullLogo() {
        RenderRequest r = RenderRequest.of(solid(10, 10, Color.RED), "x", new StampStyle());
        assertNull(r.logo());
        assertEquals("x", r.text());
    }
}
