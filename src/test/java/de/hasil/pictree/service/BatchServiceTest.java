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
    void listImagesRespectsTypeFilter(@TempDir Path src) throws Exception {
        writeJpeg(src, "foto.jpg", 10, 10);
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png",
                src.resolve("grafik.png").toFile());

        de.hasil.pictree.model.ImageTypeFilter jpegOnly = de.hasil.pictree.model.ImageTypeFilter.all()
                .withGroup("PNG", false).withGroup("GIF", false)
                .withGroup("BMP", false).withGroup("TIFF", false);
        var images = BatchService.listImages(src.toFile(), jpegOnly);
        assertEquals(1, images.size());
        assertEquals("foto.jpg", images.get(0).getName());
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

        // Ergebnisse landen im Unterordner "_stamped" des Quellordners, nicht im Album-Ordner.
        Path stamped = src.resolve(BatchService.OUTPUT_SUBDIR);
        assertEquals(stamped, result.outputDir().toPath());
        for (File out : result.saved()) {
            assertTrue(out.exists());
            assertTrue(out.getName().endsWith(".jpg"));
            assertEquals(stamped, out.getParentFile().toPath());
        }
    }

    @Test
    void writesIntoStampedSubfolderAndLeavesOriginalsUntouched(@TempDir Path src, @TempDir Path album)
            throws Exception {
        writeJpeg(src, "a.jpg", 20, 20);
        writeJpeg(src, "b.jpg", 20, 20);
        long originalCount = Files.list(src).filter(Files::isRegularFile).count();

        BatchService service = newService(album);
        BatchService.BatchResult r = service.processFolder(src.toFile(), "S", new StampStyle(), null);

        assertEquals(2, r.saved().size());
        assertTrue(Files.isDirectory(src.resolve("_stamped")));
        // Originale unverändert (gleiche Anzahl regulärer Dateien direkt im Quellordner).
        assertEquals(originalCount, Files.list(src).filter(Files::isRegularFile).count());
        // Album-Ordner bleibt leer.
        assertFalse(Files.exists(album) && Files.list(album).findAny().isPresent());
    }

    @Test
    void processFilesUsesGivenOutputDirAndSubset(@TempDir Path src, @TempDir Path out, @TempDir Path album)
            throws Exception {
        writeJpeg(src, "a.jpg", 20, 20);
        writeJpeg(src, "b.jpg", 20, 20);
        writeJpeg(src, "c.jpg", 20, 20);

        BatchService service = newService(album);
        var subset = java.util.List.of(src.resolve("a.jpg").toFile(), src.resolve("c.jpg").toFile());
        BatchService.BatchResult r = service.processFiles(
                subset, out.toFile(), "S", new StampStyle(), null, null);

        assertEquals(2, r.saved().size());
        assertEquals(out.toFile(), r.outputDir());
        for (File f : r.saved()) {
            assertEquals(out, f.getParentFile().toPath());
        }
    }

    @Test
    void secondRunDoesNotReprocessStampedOutput(@TempDir Path src, @TempDir Path album) throws Exception {
        writeJpeg(src, "a.jpg", 20, 20);
        BatchService service = newService(album);

        service.processFolder(src.toFile(), "S", new StampStyle(), null);
        // Zweiter Lauf: listImages ist nicht rekursiv -> nur das Original, nicht die _stamped-Ausgabe.
        BatchService.BatchResult second = service.processFolder(src.toFile(), "S", new StampStyle(), null);
        assertEquals(1, second.saved().size());
        // Kollisionsfreie Namensvergabe -> zweite Ausgabe mit Suffix.
        assertTrue(second.saved().get(0).getName().matches("a(-\\d+)?\\.jpg"));
    }

    @Test
    void emptyOrNonFolderYieldsEmptyResult(@TempDir Path album) {
        BatchService service = newService(album);
        BatchService.BatchResult r = service.processFolder(
                new File("Z:/gibt/es/nicht"), "x", new StampStyle(), null);
        assertEquals(0, r.total());
    }
}
