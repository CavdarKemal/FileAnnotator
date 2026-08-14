package de.hasil.pictree.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

/**
 * Ein Knoten im Datei-Baum. Kapselt eine {@link File}-Referenz und lädt seine
 * Kinder erst bei Bedarf (lazy). Ordner werden vor Dateien und jeweils
 * alphabetisch (case-insensitive) einsortiert – wie im Windows-Explorer.
 *
 * <p>Performance: {@code isDirectory()} (ein Dateisystem-Stat) wird pro Eintrag
 * genau einmal ermittelt und gecacht; Sortierung und Filter arbeiten danach
 * ohne weitere Stats. Anzeigename (teurer Shell-Call unter Windows) wird
 * ebenfalls einmalig berechnet und zwischengespeichert.
 */
public class FileTreeNode {

    private final File file;
    private final boolean syntheticRoot;
    private final FileSystemView fsv;
    /** Optionaler Bildtyp-Filter; {@code null} = ungefiltert (alle Dateien anzeigen). */
    private final ImageTypeFilter filter;

    /** Cache: ist dieser Knoten ein Verzeichnis? (ein Stat) */
    private Boolean directory;
    /** Cache: System-Anzeigename. */
    private String displayName;
    private List<FileTreeNode> children;

    /** Erzeugt den synthetischen Wurzelknoten (listet die Dateisystem-Roots/Laufwerke). */
    public static FileTreeNode createComputerRoot() {
        return createComputerRoot(null);
    }

    /** Wie {@link #createComputerRoot()}, aber mit Bildtyp-Filter für die Kinderlisten. */
    public static FileTreeNode createComputerRoot(ImageTypeFilter filter) {
        return new FileTreeNode(null, true, FileSystemView.getFileSystemView(), filter);
    }

    /** Erzeugt einen Knoten für ein konkretes Verzeichnis (z. B. für Tests). */
    public static FileTreeNode forDirectory(File dir) {
        return forDirectory(dir, null);
    }

    /** Wie {@link #forDirectory(File)}, aber mit Bildtyp-Filter. */
    public static FileTreeNode forDirectory(File dir, ImageTypeFilter filter) {
        return new FileTreeNode(dir, false, FileSystemView.getFileSystemView(), filter);
    }

    FileTreeNode(File file, boolean syntheticRoot, FileSystemView fsv) {
        this(file, syntheticRoot, fsv, null);
    }

    FileTreeNode(File file, boolean syntheticRoot, FileSystemView fsv, ImageTypeFilter filter) {
        this.file = file;
        this.syntheticRoot = syntheticRoot;
        this.fsv = fsv;
        this.filter = filter;
    }

    /** Die gekapselte Datei; {@code null} beim synthetischen Wurzelknoten. */
    public File getFile() {
        return file;
    }

    public boolean isSyntheticRoot() {
        return syntheticRoot;
    }

    /** Ist dieser Knoten ein Verzeichnis? Ergebnis wird gecacht (ein Stat). */
    private boolean isDirectory() {
        if (directory == null) {
            directory = file != null && file.isDirectory();
        }
        return directory;
    }

    /** Ein Blatt ist alles, was kein Verzeichnis ist. Der Wurzelknoten ist nie ein Blatt. */
    public boolean isLeaf() {
        if (syntheticRoot) {
            return false;
        }
        return !isDirectory();
    }

    /** True, wenn die Kinder bereits geladen (gecacht) sind – löst keinen Ladevorgang aus. */
    public boolean isChildrenLoaded() {
        return children != null;
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
        if (syntheticRoot) {
            File[] roots = File.listRoots();
            if (roots == null || roots.length == 0) {
                roots = fsv.getRoots();
            }
            if (roots == null) {
                return Collections.emptyList();
            }
            // Laufwerke sind bereits sinnvoll sortiert und stets Verzeichnisse.
            List<FileTreeNode> result = new ArrayList<>(roots.length);
            for (File f : roots) {
                result.add(childNode(f, Boolean.TRUE));
            }
            return result;
        }
        if (!isDirectory()) {
            return Collections.emptyList();
        }
        File[] entries = file.listFiles();
        if (entries == null) {
            return Collections.emptyList();
        }
        // Ein isDirectory()-Stat pro Eintrag; danach ohne weitere Stats filtern und sortieren.
        List<Entry> kept = new ArrayList<>(entries.length);
        for (File f : entries) {
            String name = f.getName();
            if (name.endsWith(".pictree.properties")) {
                continue; // Annotations-Sidecar nicht anzeigen.
            }
            boolean isDir = f.isDirectory();
            // Bildtyp-Filter: Verzeichnisse immer; Dateien nur passende Bildendungen (kein Stat).
            if (!isDir && filter != null && !filter.matchesImageName(name)) {
                continue;
            }
            kept.add(new Entry(f, isDir, name));
        }
        kept.sort(ENTRY_ORDER);
        List<FileTreeNode> result = new ArrayList<>(kept.size());
        for (Entry e : kept) {
            result.add(childNode(e.file, e.isDir));
        }
        return result;
    }

    /** Erzeugt einen Kindknoten mit bereits bekanntem Verzeichnis-Status (spart einen Stat). */
    private FileTreeNode childNode(File f, Boolean isDir) {
        FileTreeNode node = new FileTreeNode(f, false, fsv, filter);
        node.directory = isDir;
        return node;
    }

    /** Anzeigename gemäß System (z. B. "Lokaler Datenträger (C:)"), einmalig ermittelt. */
    public String getDisplayName() {
        if (syntheticRoot) {
            return "Dieser PC";
        }
        if (displayName == null) {
            String name = fsv.getSystemDisplayName(file);
            if (name == null || name.isBlank()) {
                name = file.getName();
            }
            if (name == null || name.isBlank()) {
                name = file.getAbsolutePath();
            }
            displayName = name;
        }
        return displayName;
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

    /** Vorsortier-Eintrag mit vorab ermitteltem Verzeichnis-Status – vermeidet Stats im Comparator. */
    private static final class Entry {
        final File file;
        final boolean isDir;
        final String name;

        Entry(File file, boolean isDir, String name) {
            this.file = file;
            this.isDir = isDir;
            this.name = name;
        }
    }

    /** Ordner zuerst, dann Dateien; innerhalb der Gruppe alphabetisch (ohne Dateisystem-Stats). */
    private static final Comparator<Entry> ENTRY_ORDER = (a, b) -> {
        if (a.isDir != b.isDir) {
            return a.isDir ? -1 : 1;
        }
        return a.name.compareToIgnoreCase(b.name);
    };
}
