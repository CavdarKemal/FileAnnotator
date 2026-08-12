package de.hasil.pictree.ui;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
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

    /**
     * Selektiert (und expandiert) den Pfad zur angegebenen Datei/Ordner, sofern
     * erreichbar. Löst die Selektions-Listener aus.
     *
     * @return {@code true}, wenn zumindest teilweise navigiert werden konnte
     */
    public boolean selectPath(File target) {
        if (target == null) {
            return false;
        }
        File abs = target.getAbsoluteFile();
        // Kette Wurzel->Ziel (root-first).
        Deque<File> stack = new ArrayDeque<>();
        for (File f = abs; f != null; f = f.getParentFile()) {
            stack.push(f);
        }
        List<File> chain = new ArrayList<>(stack);

        if (!(tree.getModel().getRoot() instanceof FileTreeNode rootNode)) {
            return false;
        }
        // Ab welchem Kettenglied unterhalb der Baum-Wurzel gesucht wird.
        int startIndex = 0;
        if (rootNode.getFile() != null) {
            int idx = chain.indexOf(rootNode.getFile());
            if (idx < 0) {
                return false; // Ziel liegt nicht unterhalb der Wurzel
            }
            startIndex = idx + 1;
        }

        List<Object> nodes = new ArrayList<>();
        nodes.add(rootNode);
        FileTreeNode current = rootNode;
        for (int i = startIndex; i < chain.size(); i++) {
            File step = chain.get(i);
            FileTreeNode next = null;
            for (FileTreeNode child : current.getChildren()) {
                if (step.equals(child.getFile())) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                break;
            }
            nodes.add(next);
            current = next;
        }
        if (nodes.size() <= 1) {
            return false;
        }
        TreePath path = new TreePath(nodes.toArray());
        tree.expandPath(path);
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        return true;
    }

    public JTree getTree() {
        return tree;
    }
}
