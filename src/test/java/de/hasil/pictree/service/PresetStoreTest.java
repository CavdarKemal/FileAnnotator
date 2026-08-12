package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.hasil.pictree.model.StampStyle;

class PresetStoreTest {

    private StampStyle style(Color c, float size) {
        StampStyle s = new StampStyle();
        s.setColor(c);
        s.setRelativeSize(size);
        return s;
    }

    @Test
    void saveLoadAndListNames(@TempDir Path dir) {
        Path file = dir.resolve("presets.properties");
        PresetStore store = new PresetStore(file);
        store.save("Copyright", style(Color.RED, 0.08f));
        store.save("Titel", style(Color.WHITE, 0.2f));

        assertEquals(java.util.List.of("Copyright", "Titel"), store.names());
        StampStyle loaded = store.load("Copyright").orElseThrow();
        assertEquals(Color.RED, loaded.getColor());
        assertEquals(0.08f, loaded.getRelativeSize(), 1e-6);
    }

    @Test
    void persistsAcrossInstances(@TempDir Path dir) {
        Path file = dir.resolve("presets.properties");
        new PresetStore(file).save("A", style(Color.GREEN, 0.1f));

        PresetStore reopened = new PresetStore(file);
        assertTrue(reopened.names().contains("A"));
        assertEquals(Color.GREEN, reopened.load("A").orElseThrow().getColor());
    }

    @Test
    void deleteRemovesPreset(@TempDir Path dir) {
        PresetStore store = new PresetStore(dir.resolve("presets.properties"));
        store.save("X", style(Color.BLUE, 0.1f));
        store.save("Y", style(Color.BLACK, 0.1f));
        store.delete("X");
        assertFalse(store.names().contains("X"));
        assertTrue(store.names().contains("Y"));
        assertTrue(store.load("X").isEmpty());
    }

    @Test
    void overwriteUpdatesExisting(@TempDir Path dir) {
        PresetStore store = new PresetStore(dir.resolve("presets.properties"));
        store.save("P", style(Color.RED, 0.1f));
        store.save("P", style(Color.CYAN, 0.3f));
        assertEquals(1, store.names().size());
        assertEquals(Color.CYAN, store.load("P").orElseThrow().getColor());
    }
}
