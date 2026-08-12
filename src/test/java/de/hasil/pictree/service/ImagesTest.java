package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class ImagesTest {

    @Test
    void downscaleReducesLongerEdgeToMax() {
        BufferedImage src = new BufferedImage(4000, 2000, BufferedImage.TYPE_INT_RGB);
        BufferedImage out = Images.downscaleToMax(src, 1600);
        assertEquals(1600, out.getWidth());
        assertEquals(800, out.getHeight());
    }

    @Test
    void downscalePreservesAspectForPortrait() {
        BufferedImage src = new BufferedImage(1000, 3000, BufferedImage.TYPE_INT_RGB);
        BufferedImage out = Images.downscaleToMax(src, 600);
        assertEquals(600, out.getHeight());
        assertEquals(200, out.getWidth());
    }

    @Test
    void smallImageReturnedUnchanged() {
        BufferedImage src = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        assertSame(src, Images.downscaleToMax(src, 1600));
    }

    @Test
    void nullAndInvalidAreSafe() {
        assertTrue(Images.downscaleToMax(null, 100) == null);
        BufferedImage src = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        assertSame(src, Images.downscaleToMax(src, 0));
    }
}
