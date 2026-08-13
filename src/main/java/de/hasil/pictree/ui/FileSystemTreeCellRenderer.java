package de.hasil.pictree.ui;

import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;

import de.hasil.pictree.model.FileTreeNode;

/**
 * Renderer, der System-Icons und Anzeigenamen (Explorer-Optik) für
 * {@link FileTreeNode}s liefert.
 *
 * <p>System-Icons werden pro Datei gecacht: {@code getSystemIcon} ist unter
 * Windows ein teurer Shell-Call und würde sonst bei jedem Repaint erneut laufen.
 */
public class FileSystemTreeCellRenderer extends DefaultTreeCellRenderer {

    private final transient FileSystemView fsv = FileSystemView.getFileSystemView();
    private final transient Map<File, Icon> iconCache = new HashMap<>();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        if (value instanceof FileTreeNode node) {
            setText(node.getDisplayName());
            if (!node.isSyntheticRoot() && node.getFile() != null) {
                Icon icon = iconFor(node.getFile());
                if (icon != null) {
                    setIcon(icon);
                }
            }
        }
        return this;
    }

    private Icon iconFor(File file) {
        Icon cached = iconCache.get(file);
        if (cached != null) {
            return cached;
        }
        try {
            Icon icon = fsv.getSystemIcon(file);
            if (icon != null) {
                iconCache.put(file, icon);
            }
            return icon;
        } catch (Exception ignored) {
            // Icon-Ermittlung darf das Rendering nie sprengen.
            return null;
        }
    }
}
