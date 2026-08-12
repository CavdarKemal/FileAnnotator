package de.hasil.pictree.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import de.hasil.pictree.model.StampStyle;

/**
 * Gemeinsame, deterministische Eingaben für die Golden-Image-Tests. Bei einer
 * bewussten Rendering-Änderung kann das Referenzbild über {@code writeGolden}
 * neu erzeugt werden.
 */
final class GoldenFixtures {

    static final int WIDTH = 400;
    static final int HEIGHT = 300;
    static final String TEXT = "PicTree";

    private GoldenFixtures() {
    }

    static BufferedImage base() {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(20, 40, 90));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.dispose();
        return img;
    }

    static StampStyle style() {
        StampStyle s = new StampStyle();
        s.setFontFamily("Roboto");
        s.setColor(Color.WHITE);
        s.setRelativeSize(0.12f);
        s.setRelX(0.5);
        s.setRelY(0.5);
        s.setOutline(true);
        return s;
    }

    static BufferedImage render() {
        return ImageStampService.renderStamp(base(), TEXT, style());
    }
}
