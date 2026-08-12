package de.hasil.pictree.model;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;

/**
 * {@link TreeModel} über dem Dateisystem. Kinder werden über {@link FileTreeNode}
 * lazy geladen. Das Modell ist read-only; {@link #valueForPathChanged} wird nicht
 * unterstützt.
 */
public class LazyFileTreeModel implements TreeModel {

    private final FileTreeNode root;

    public LazyFileTreeModel() {
        this(FileTreeNode.createComputerRoot());
    }

    public LazyFileTreeModel(FileTreeNode root) {
        this.root = root;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((FileTreeNode) parent).getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return ((FileTreeNode) parent).getChildCount();
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((FileTreeNode) node).isLeaf();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) {
            return -1;
        }
        return ((FileTreeNode) parent).getIndexOfChild((FileTreeNode) child);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // Read-only-Modell: nichts zu tun.
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        // Statisches Modell: keine Änderungs-Events.
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // Statisches Modell: keine Änderungs-Events.
    }
}
