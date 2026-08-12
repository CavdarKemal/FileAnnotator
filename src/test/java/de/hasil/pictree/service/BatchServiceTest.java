package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.hasil.pictree.model.StampStyle;

class BatchServiceTest {

    private void writeJpeg(Path dir, String name, int w, int h) throws Exception {
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "jpg",
                dir.resolve(name).toFile());
    }

    private BatchService newService(Path album) {
        return new BatchService(new SaveService(album), new ExifService());
    }

    @Test
    void listImagesFiltersAndSorts(@TempDir Path src) throws Exception {
        writeJpeg(src, "b.jpg", 10, 10);
        writeJpeg(src, "a.jpg", 10, 10);
        Files.writeString(src.resolve("notiz.txt"), "kein Bild");
        Files.createDirectory(src.resolve("unterordner"));

        var images = BatchService.listImages(src.toFile());
        assertEquals(2, images.size());
        assertEquals("a.jpg", images.get(0).getName());
        assertEquals("b.jpg", images.get(1).getName());
    }

    @Test
    void processesAllImagesAndSkipsNonImages(@TempDir Path src, @TempDir Path album) throws Exception {
        writeJpeg(src, "one.jpg", 40, 30);
        writeJpeg(src, "two.jpg", 30, 40);
        writeJpeg(src, "three.jpg", 20, 20);
        Files.writeString(src.resolve("readme.txt"), "text");
        // Gefälschtes Bild: richtige Endung, aber kein gültiger Inhalt.
        Files.writeString(src.resolve("fake.png"), "keinBild");

        AtomicInteger progressCalls = new AtomicInteger();
        StampStyle style = new StampStyle();

        BatchService service = newService(album);
        BatchService.BatchResult result = service.processFolder(
                src.toFile(), "Stempel", style,
                (done, total, current, saved) -> progressCalls.incrementAndGet());

        // 3 echte JPGs + 1 fake.png werden als Bild-Kandidaten erkannt (Endung),
        // fake.png scheitert beim Laden -> failed.
        assertEquals(3, result.saved().size());
        assertEquals(1, result.failed().size());
        assertEquals(4, result.total());
        assertEquals(4, progressCalls.get());
        // Fehlgeschlagenes Bild trägt eine Begründung.
        assertFalse(result.failed().get(0).reason().isBlank());

        for (File out : result.saved()) {
            assertTrue(out.exists());
            assertTrue(out.getName().endsWith(".jpg"));
            assertEquals(album, out.getParentFile().toPath());
        }
    }

    @Test
    void emptyOrNonFolderYieldsEmptyResult(@TempDir Path album) {
        BatchService service = newService(album);
        BatchService.BatchResult r = service.processFolder(
                new File("Z:/gibt/es/nicht"), "x", new StampStyle(), null);
        assertEquals(0, r.total());
    }
}
