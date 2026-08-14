package de.hasil.pictree.ui;

import java.awt.Cursor;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import de.hasil.pictree.model.FileTreeNode;
import de.hasil.pictree.model.LazyFileTreeModel;

/**
 * Baum-Panel im Explorer-Stil. Zeigt das Dateisystem, meldet Datei- und
 * Ordner-Selektionen an registrierte Listener.
 *
 * <p>Das Aufklappen eines noch nicht geladenen Ordners läuft asynchron: die
 * (potenziell teure) Kinderliste wird im Hintergrund ermittelt (Warte-Cursor),
 * danach wird der Pfad automatisch expandiert – so blockiert der EDT nie.
 */
public class FileTreePanel extends JScrollPane {

    private final JTree tree;
    private final List<Consumer<File>> selectionListeners = new ArrayList<>();
    /** Knoten, deren Kinder gerade im Hintergrund geladen werden (Identitäts-Set). */
    private final transient Set<FileTreeNode> loading = Collections.newSetFromMap(new IdentityHashMap<>());

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
        installAsyncExpansion();
        setViewportView(tree);
    }

    /** Lädt Kinder beim Aufklappen im Hintergrund, statt den EDT zu blockieren. */
    private void installAsyncExpansion() {
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object last = event.getPath().getLastPathComponent();
                if (!(last instanceof FileTreeNode node)) {
                    return;
                }
                if (node.isLeaf() || node.isChildrenLoaded() || loading.contains(node)) {
                    return; // bereits geladen oder in Arbeit -> normal aufklappen
                }
                // Diese Expansion abbrechen; Kinder im Hintergrund laden, danach erneut aufklappen.
                loading.add(node);
                updateBusyCursor();
                final TreePath path = event.getPath();
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        node.getChildren(); // teures listFiles + Stats off-EDT
                        return null;
                    }

                    @Override
                    protected void done() {
                        loading.remove(node);
                        updateBusyCursor();
                        tree.expandPath(path);
                    }
                }.execute();
                throw new ExpandVetoException(event);
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
                // Einklappen ist immer erlaubt.
            }
        });
    }

    private void updateBusyCursor() {
        tree.setCursor(Cursor.getPredefinedCursor(
                loading.isEmpty() ? Cursor.DEFAULT_CURSOR : Cursor.WAIT_CURSOR));
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

    /**
     * Ersetzt das Baum-Modell (z. B. nach einer Filteränderung) und stellt – so
     * weit erreichbar – die zuvor selektierte Datei/den Ordner wieder her.
     */
    public void rebuildPreservingSelection(LazyFileTreeModel newModel) {
        File previouslySelected = getSelectedFile();
        tree.setModel(newModel);
        if (previouslySelected != null) {
            selectPath(previouslySelected);
        }
    }

    public JTree getTree() {
        return tree;
    }
}
