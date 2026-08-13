package de.hasil.pictree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageTypeFilterTest {

    @Test
    void allAcceptsEverySupportedExtension() {
        ImageTypeFilter all = ImageTypeFilter.all();
        assertTrue(all.isAllActive());
        assertFalse(all.isEmpty());
        for (String ext : new String[] {"jpg", "jpeg", "png", "gif", "bmp", "wbmp", "tif", "tiff"}) {
            assertTrue(all.extensions().contains(ext), "fehlt: " + ext);
        }
    }

    @Test
    void withGroupTogglesOnlyThatGroup() {
        ImageTypeFilter noPng = ImageTypeFilter.all().withGroup("PNG", false);
        assertFalse(noPng.isGroupActive("PNG"));
        assertTrue(noPng.isGroupActive("JPEG"));
        assertFalse(noPng.extensions().contains("png"));
        assertTrue(noPng.extensions().contains("jpg"));
        assertFalse(noPng.isAllActive());
    }

    @Test
    void acceptsImageChecksFileAndExtension(@TempDir Path dir) throws Exception {
        File png = Files.writeString(dir.resolve("a.png"), "x").toFile();
        File jpg = Files.writeString(dir.resolve("b.jpg"), "x").toFile();
        File txt = Files.writeString(dir.resolve("c.txt"), "x").toFile();

        ImageTypeFilter jpegOnly = ImageTypeFilter.all()
                .withGroup("PNG", false).withGroup("GIF", false)
                .withGroup("BMP", false).withGroup("TIFF", false);
        assertTrue(jpegOnly.acceptsImage(jpg));
        assertFalse(jpegOnly.acceptsImage(png));
        assertFalse(jpegOnly.acceptsImage(txt));
        // Verzeichnis ist kein Bild, aber im Baum erlaubt.
        assertFalse(jpegOnly.acceptsImage(dir.toFile()));
        assertTrue(jpegOnly.acceptsInTree(dir.toFile()));
        assertFalse(jpegOnly.acceptsInTree(png));
        assertTrue(jpegOnly.acceptsInTree(jpg));
    }

    @Test
    void emptyFilterAcceptsNoImagesButStillFolders(@TempDir Path dir) {
        ImageTypeFilter empty = ImageTypeFilter.all()
                .withGroup("JPEG", false).withGroup("PNG", false).withGroup("GIF", false)
                .withGroup("BMP", false).withGroup("TIFF", false);
        assertTrue(empty.isEmpty());
        assertTrue(empty.extensions().isEmpty());
        assertTrue(empty.acceptsInTree(dir.toFile()));
    }

    @Test
    void encodeDecodeRoundTrip() {
        ImageTypeFilter f = ImageTypeFilter.all().withGroup("GIF", false).withGroup("BMP", false);
        ImageTypeFilter back = ImageTypeFilter.decode(f.encode());
        assertEquals(f, back);
        assertTrue(back.isGroupActive("JPEG"));
        assertFalse(back.isGroupActive("GIF"));
    }

    @Test
    void decodeBlankOrNullYieldsAll() {
        assertEquals(ImageTypeFilter.all(), ImageTypeFilter.decode(null));
        assertEquals(ImageTypeFilter.all(), ImageTypeFilter.decode(""));
        assertEquals(ImageTypeFilter.all(), ImageTypeFilter.decode("  "));
        // Unbekannte Gruppennamen werden ignoriert -> keine gültige Gruppe -> all().
        assertEquals(ImageTypeFilter.all(), ImageTypeFilter.decode("SVG,EPS"));
    }
}
