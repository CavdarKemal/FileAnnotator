package de.hasil.pictree.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

/**
 * Ein Knoten im Datei-Baum. Kapselt eine {@link File}-Referenz und lädt seine
 * Kinder erst bei Bedarf (lazy). Ordner werden vor Dateien und jeweils
 * alphabetisch (case-insensitive) einsortiert – wie im Windows-Explorer.
 */
public class FileTreeNode {

    /** Ordner zuerst, dann Dateien; innerhalb der Gruppe alphabetisch. */
    static final Comparator<File> EXPLORER_ORDER = (a, b) -> {
        boolean da = a.isDirectory();
        boolean db = b.isDirectory();
        if (da != db) {
            return da ? -1 : 1;
        }
        return a.getName().compareToIgnoreCase(b.getName());
    };

    private final File file;
    private final boolean syntheticRoot;
    private final FileSystemView fsv;
    private List<FileTreeNode> children;

    /** Erzeugt den synthetischen Wurzelknoten (listet die Dateisystem-Roots/Laufwerke). */
    public static FileTreeNode createComputerRoot() {
        return new FileTreeNode(null, true, FileSystemView.getFileSystemView());
    }

    /** Erzeugt einen Knoten für ein konkretes Verzeichnis (z. B. für Tests). */
    public static FileTreeNode forDirectory(File dir) {
        return new FileTreeNode(dir, false, FileSystemView.getFileSystemView());
    }

    FileTreeNode(File file, boolean syntheticRoot, FileSystemView fsv) {
        this.file = file;
        this.syntheticRoot = syntheticRoot;
        this.fsv = fsv;
    }

    /** Die gekapselte Datei; {@code null} beim synthetischen Wurzelknoten. */
    public File getFile() {
        return file;
    }

    public boolean isSyntheticRoot() {
        return syntheticRoot;
    }

    /** Ein Blatt ist alles, was kein Verzeichnis ist. Der Wurzelknoten ist nie ein Blatt. */
    public boolean isLeaf() {
        if (syntheticRoot) {
            return false;
        }
        return !file.isDirectory();
    }

    /** Lazy geladene, sortierte Kinderliste. Bei fehlendem Zugriff leer. */
    public List<FileTreeNode> getChildren() {
        if (children == null) {
            children = loadChildren();
        }
        return children;
    }

    public int getChildCount() {
        return getChildren().size();
    }

    public FileTreeNode getChildAt(int index) {
        return getChildren().get(index);
    }

    public int getIndexOfChild(FileTreeNode child) {
        return getChildren().indexOf(child);
    }

    private List<FileTreeNode> loadChildren() {
        File[] raw;
        if (syntheticRoot) {
            raw = fsv.getRoots();
            if (raw == null || raw.length == 0) {
                raw = File.listRoots();
            }
        } else if (file.isDirectory()) {
            raw = file.listFiles();
        } else {
            raw = null;
        }
        if (raw == null) {
            return Collections.emptyList();
        }
        File[] sorted = Arrays.copyOf(raw, raw.length);
        // Der synthetische Root liefert bereits sinnvoll sortierte Roots; nur Verzeichnisinhalte sortieren.
        if (!syntheticRoot) {
            Arrays.sort(sorted, EXPLORER_ORDER);
        }
        List<FileTreeNode> result = new ArrayList<>(sorted.length);
        for (File f : sorted) {
            result.add(new FileTreeNode(f, false, fsv));
        }
        return result;
    }

    /** Anzeigename gemäß System (z. B. "Lokaler Datenträger (C:)"). */
    public String getDisplayName() {
        if (syntheticRoot) {
            return "Dieser PC";
        }
        String name = fsv.getSystemDisplayName(file);
        if (name == null || name.isBlank()) {
            name = file.getName();
        }
        if (name == null || name.isBlank()) {
            name = file.getAbsolutePath();
        }
        return name;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileTreeNode other)) {
            return false;
        }
        if (syntheticRoot != other.syntheticRoot) {
            return false;
        }
        if (file == null) {
            return other.file == null;
        }
        return file.equals(other.file);
    }

    @Override
    public int hashCode() {
        return file == null ? Boolean.hashCode(syntheticRoot) : file.hashCode();
    }
}
