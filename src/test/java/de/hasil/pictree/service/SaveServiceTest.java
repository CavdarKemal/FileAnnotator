package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveServiceTest {

    private BufferedImage image(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void savesReadableJpgAndCreatesDir(@TempDir Path dir) throws IOException {
        Path album = dir.resolve("album-nicht-existent");
        SaveService service = new SaveService(album);

        File out = service.save(image(64, 48), "foto.png");
        assertTrue(out.exists());
        assertTrue(out.getName().endsWith(".jpg"));
        assertEquals("foto.jpg", out.getName());

        BufferedImage reread = ImageIO.read(out);
        assertNotNull(reread);
        assertEquals(64, reread.getWidth());
        assertEquals(48, reread.getHeight());
    }

    @Test
    void collisionFreeNaming(@TempDir Path dir) throws IOException {
        SaveService service = new SaveService(dir);
        File first = service.save(image(10, 10), "bild.jpg");
        File second = service.save(image(10, 10), "bild.jpg");
        File third = service.save(image(10, 10), "bild.jpg");

        assertEquals("bild.jpg", first.getName());
        assertEquals("bild-1.jpg", second.getName());
        assertEquals("bild-2.jpg", third.getName());
    }

    @Test
    void defaultAlbumDirEndsWithExpectedSubpath() {
        Path def = SaveService.defaultAlbumDir();
        String s = def.toString().replace('\\', '/');
        assertTrue(s.endsWith("Pictures/PicTreeAlbums"), s);
    }
}
