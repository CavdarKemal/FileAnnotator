package de.hasil.pictree.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;

import de.hasil.pictree.model.FileTreeNode;
import de.hasil.pictree.model.LazyFileTreeModel;

/**
 * Baum-Panel im Explorer-Stil. Zeigt das Dateisystem, meldet Datei- und
 * Ordner-Selektionen an registrierte Listener.
 */
public class FileTreePanel extends JScrollPane {

    private final JTree tree;
    private final List<Consumer<File>> selectionListeners = new ArrayList<>();

    public FileTreePanel() {
        this(new LazyFileTreeModel());
    }

    public FileTreePanel(LazyFileTreeModel model) {
        this.tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new FileSystemTreeCellRenderer());
        tree.addTreeSelectionListener(e -> fireSelection());
        setViewportView(tree);
    }

    private void fireSelection() {
        File selected = getSelectedFile();
        for (Consumer<File> listener : selectionListeners) {
            listener.accept(selected);
        }
    }

    /** Aktuell selektierte Datei/Ordner oder {@code null}. */
    public File getSelectedFile() {
        Object last = tree.getLastSelectedPathComponent();
        if (last instanceof FileTreeNode node) {
            return node.getFile();
        }
        return null;
    }

    /** Registriert einen Listener, der bei jeder Selektionsänderung die Datei erhält (ggf. null). */
    public void addFileSelectionListener(Consumer<File> listener) {
        selectionListeners.add(listener);
    }

    public JTree getTree() {
        return tree;
    }
}
