package de.hasil.pictree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.hasil.pictree.model.FileTreeNode;
import de.hasil.pictree.model.LazyFileTreeModel;

class FileTreePanelTest {

    private FileTreePanel panelRootedAt(Path dir) {
        LazyFileTreeModel model = new LazyFileTreeModel(FileTreeNode.forDirectory(dir.toFile()));
        return new FileTreePanel(model);
    }

    @Test
    void selectPathSelectsNestedFile(@TempDir Path dir) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "GUI-Test in Headless übersprungen");
        Path sub = Files.createDirectories(dir.resolve("a/b"));
        File target = Files.writeString(sub.resolve("foto.jpg"), "x").toFile();

        FileTreePanel panel = panelRootedAt(dir);
        assertTrue(panel.selectPath(target));
        assertEquals(target.getAbsoluteFile(), panel.getSelectedFile().getAbsoluteFile());
    }

    @Test
    void selectPathReturnsFalseForOutsideTarget(@TempDir Path dir, @TempDir Path other) {
        assumeFalse(GraphicsEnvironment.isHeadless(), "GUI-Test in Headless übersprungen");
        FileTreePanel panel = panelRootedAt(dir);
        // Ziel liegt nicht unterhalb der Baum-Wurzel.
        assertFalse(panel.selectPath(other.resolve("x.jpg").toFile()));
    }
}
