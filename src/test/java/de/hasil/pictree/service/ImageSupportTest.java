package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageSupportTest {

    @Test
    void extensionDetection() {
        assertEquals("jpg", ImageSupport.extensionOf("foto.JPG"));
        assertEquals("png", ImageSupport.extensionOf("a.b.png"));
        assertEquals("", ImageSupport.extensionOf("ohneEndung"));
        assertEquals("", ImageSupport.extensionOf("endetMitPunkt."));
    }

    @Test
    void isImageFileByExtension(@TempDir Path dir) throws IOException {
        File png = Files.writeString(dir.resolve("x.png"), "nichtWirklichPng").toFile();
        File txt = Files.writeString(dir.resolve("x.txt"), "text").toFile();
        assertTrue(ImageSupport.isImageFile(png));
        assertFalse(ImageSupport.isImageFile(txt));
        assertFalse(ImageSupport.isImageFile(dir.toFile())); // Verzeichnis
    }

    @Test
    void loadReadsRealImageAndRejectsFake(@TempDir Path dir) throws IOException {
        File real = dir.resolve("real.png").toFile();
        ImageIO.write(new BufferedImage(10, 8, BufferedImage.TYPE_INT_RGB), "png", real);
        BufferedImage img = ImageSupport.load(real);
        assertNotNull(img);
        assertEquals(10, img.getWidth());

        File fake = Files.writeString(dir.resolve("fake.png"), "keinBild").toFile();
        assertEquals(null, ImageSupport.load(fake));
    }
}
