package de.hasil.pictree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainFrameDropTest {

    @Test
    void picksFirstExistingFile(@TempDir Path dir) throws Exception {
        File missing = dir.resolve("weg.jpg").toFile();
        File real = Files.writeString(dir.resolve("da.jpg"), "x").toFile();
        assertEquals(real, MainFrame.pickDropTarget(List.of(missing, real)));
    }

    @Test
    void picksFolder(@TempDir Path dir) {
        assertEquals(dir.toFile(), MainFrame.pickDropTarget(List.of(dir.toFile())));
    }

    @Test
    void nullOrEmptyYieldsNull(@TempDir Path dir) {
        assertNull(MainFrame.pickDropTarget(null));
        assertNull(MainFrame.pickDropTarget(List.of(dir.resolve("gibtsnicht").toFile())));
    }
}
