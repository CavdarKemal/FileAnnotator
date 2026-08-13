package de.hasil.pictree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * EDT-Smoke-Test für den Thumbnail-Streifen (ohne AssertJ-Swing). Prüft die
 * synchron aufgebaute Zellstruktur und die Auswahl-Logik; das asynchrone Laden
 * der Bilder wird nicht abgewartet.
 */
class ThumbnailStripPanelTest {

    private File writeJpeg(Path dir, String name) throws Exception {
        File f = dir.resolve(name).toFile();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpg", f);
        return f;
    }

    @Test
    void setImagesSelectsAllByDefaultAndClearEmpties(@TempDir Path dir) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Headless: Thumbnail-Smoke-Test übersprungen");

        File a = writeJpeg(dir, "a.jpg");
        File b = writeJpeg(dir, "b.jpg");

        AtomicReference<ThumbnailStripPanel> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new ThumbnailStripPanel()));
        ThumbnailStripPanel strip = ref.get();

        SwingUtilities.invokeAndWait(() -> {
            strip.setImages(List.of(a, b));
            assertEquals(2, strip.getImageCount());
            // Standard: alle angehakt.
            assertEquals(2, strip.getSelectedImages().size());
            assertTrue(strip.getSelectedImages().contains(a));

            strip.clear();
            assertEquals(0, strip.getImageCount());
            assertTrue(strip.getSelectedImages().isEmpty());
        });
    }
}
