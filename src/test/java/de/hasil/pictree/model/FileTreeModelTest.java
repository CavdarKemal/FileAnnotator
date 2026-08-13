package de.hasil.pictree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTreeModelTest {

    @Test
    void childrenAreSortedFoldersFirstThenAlphabetical(@TempDir Path dir) throws IOException {
        // Absichtlich unsortiert und gemischt anlegen.
        Files.writeString(dir.resolve("banana.txt"), "x");
        Files.writeString(dir.resolve("apple.txt"), "x");
        Files.createDirectory(dir.resolve("Zeta"));
        Files.createDirectory(dir.resolve("alpha"));

        FileTreeNode root = FileTreeNode.forDirectory(dir.toFile());
        assertEquals(4, root.getChildCount());

        // Ordner zuerst (alpha, Zeta – case-insensitive), dann Dateien (apple, banana).
        assertEquals("alpha", root.getChildAt(0).getFile().getName());
        assertEquals("Zeta", root.getChildAt(1).getFile().getName());
        assertEquals("apple.txt", root.getChildAt(2).getFile().getName());
        assertEquals("banana.txt", root.getChildAt(3).getFile().getName());
    }

    @Test
    void directoriesAreNotLeavesFilesAre(@TempDir Path dir) throws IOException {
        File sub = Files.createDirectory(dir.resolve("sub")).toFile();
        File txt = Files.writeString(dir.resolve("note.txt"), "x").toFile();

        assertFalse(FileTreeNode.forDirectory(sub).isLeaf());
        assertTrue(FileTreeNode.forDirectory(txt).isLeaf());
    }

    @Test
    void modelDelegatesToNodes(@TempDir Path dir) throws IOException {
        Files.createDirectory(dir.resolve("child"));
        FileTreeNode root = FileTreeNode.forDirectory(dir.toFile());
        LazyFileTreeModel model = new LazyFileTreeModel(root);

        assertEquals(root, model.getRoot());
        assertEquals(1, model.getChildCount(root));
        Object child = model.getChild(root, 0);
        assertEquals(0, model.getIndexOfChild(root, child));
        assertFalse(model.isLeaf(child)); // "child" ist ein Verzeichnis
    }

    @Test
    void filterHidesNonMatchingImagesKeepsFoldersAndDropsNonImages(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("foto.jpg"), "x");
        Files.writeString(dir.resolve("grafik.png"), "x");
        Files.writeString(dir.resolve("notiz.txt"), "x");
        Files.createDirectory(dir.resolve("unterordner"));

        // Filter: nur JPEG -> png und txt verschwinden, Ordner bleibt.
        ImageTypeFilter jpegOnly = ImageTypeFilter.all()
                .withGroup("PNG", false).withGroup("GIF", false)
                .withGroup("BMP", false).withGroup("TIFF", false);
        FileTreeNode root = FileTreeNode.forDirectory(dir.toFile(), jpegOnly);

        assertEquals(2, root.getChildCount());
        assertEquals("unterordner", root.getChildAt(0).getFile().getName());
        assertEquals("foto.jpg", root.getChildAt(1).getFile().getName());
    }

    @Test
    void unfilteredFactoryStillShowsAllFiles(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("notiz.txt"), "x");
        Files.writeString(dir.resolve("foto.jpg"), "x");
        // Alte, filterlose Factory zeigt weiterhin auch Nicht-Bilder.
        FileTreeNode root = FileTreeNode.forDirectory(dir.toFile());
        assertEquals(2, root.getChildCount());
    }

    @Test
    void syntheticRootExposesFilesystemRoots() {
        FileTreeNode computer = FileTreeNode.createComputerRoot();
        assertTrue(computer.isSyntheticRoot());
        assertFalse(computer.isLeaf());
        // Auf jedem System existiert mindestens ein Root/Laufwerk.
        assertTrue(computer.getChildCount() >= 1);
    }
}
