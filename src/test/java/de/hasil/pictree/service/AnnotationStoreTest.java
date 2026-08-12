package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.hasil.pictree.model.Annotation;
import de.hasil.pictree.model.StampStyle;

class AnnotationStoreTest {

    private final AnnotationStore store = new AnnotationStore();

    private File imageFile(Path dir, String name) {
        return dir.resolve(name).toFile();
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path dir) throws Exception {
        File image = imageFile(dir, "foto.jpg");

        StampStyle style = new StampStyle();
        style.setColor(new Color(12, 34, 56));
        style.setFontFamily("Roboto");
        style.setFontStyle(Font.BOLD);
        style.setRelativeSize(0.11f);
        style.setRelX(0.25);
        style.setRelY(0.75);
        style.setOutline(false);

        store.save(image, "Urlaub 2024", style);
        assertTrue(store.exists(image));
        // Sidecar liegt neben dem Bild:
        assertTrue(new File(dir.toFile(), "foto.jpg" + AnnotationStore.SUFFIX).isFile());

        Optional<Annotation> loaded = store.load(image);
        assertTrue(loaded.isPresent());
        Annotation a = loaded.get();
        assertEquals("Urlaub 2024", a.comment());
        assertEquals(new Color(12, 34, 56), a.style().getColor());
        assertEquals("Roboto", a.style().getFontFamily());
        assertEquals(Font.BOLD, a.style().getFontStyle());
        assertEquals(0.11f, a.style().getRelativeSize(), 1e-6);
        assertEquals(0.25, a.style().getRelX(), 1e-9);
        assertEquals(0.75, a.style().getRelY(), 1e-9);
        assertFalse(a.style().isOutline());
    }

    @Test
    void loadMissingReturnsEmpty(@TempDir Path dir) {
        assertTrue(store.load(imageFile(dir, "gibtsnicht.jpg")).isEmpty());
    }

    @Test
    void deleteRemovesSidecar(@TempDir Path dir) throws Exception {
        File image = imageFile(dir, "x.jpg");
        store.save(image, "hallo", new StampStyle());
        assertTrue(store.exists(image));
        assertTrue(store.delete(image));
        assertFalse(store.exists(image));
        assertFalse(store.delete(image)); // zweites Mal: nichts mehr zu löschen
    }

    @Test
    void colorAlphaIsPreserved(@TempDir Path dir) throws Exception {
        File image = imageFile(dir, "alpha.jpg");
        StampStyle style = new StampStyle();
        style.setColor(new Color(10, 20, 30, 128)); // mit Alpha
        store.save(image, "c", style);

        Color loaded = store.load(image).orElseThrow().style().getColor();
        assertEquals(128, loaded.getAlpha());
        assertEquals(10, loaded.getRed());
    }
}
