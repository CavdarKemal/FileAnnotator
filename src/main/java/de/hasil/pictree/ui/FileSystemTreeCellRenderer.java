package de.hasil.pictree.ui;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;

import de.hasil.pictree.model.FileTreeNode;

/**
 * Renderer, der System-Icons und Anzeigenamen (Explorer-Optik) für
 * {@link FileTreeNode}s liefert.
 */
public class FileSystemTreeCellRenderer extends DefaultTreeCellRenderer {

    private final FileSystemView fsv = FileSystemView.getFileSystemView();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        if (value instanceof FileTreeNode node) {
            setText(node.getDisplayName());
            if (!node.isSyntheticRoot() && node.getFile() != null) {
                try {
                    Icon icon = fsv.getSystemIcon(node.getFile());
                    if (icon != null) {
                        setIcon(icon);
                    }
                } catch (Exception ignored) {
                    // Icon-Ermittlung darf das Rendering nie sprengen.
                }
            }
        }
        return this;
    }
}
