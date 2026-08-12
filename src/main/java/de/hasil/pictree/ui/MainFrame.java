package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import de.hasil.pictree.App;
import de.hasil.pictree.model.StampStyle;

/**
 * Hauptfenster: links der Datei-Baum, rechts Werkzeugleiste, Bildvorschau und
 * Kommentarfeld. Ein gemeinsames {@link StampStyle} verbindet Toolbar und
 * Vorschau (Live-Preview).
 */
public class MainFrame extends JFrame {

    private final FileTreePanel treePanel;
    private final PreviewPanel previewPanel;
    private final StyleToolbar styleToolbar;
    private final CommentPanel commentPanel;
    private final JLabel statusLabel;
    private final StampStyle style = new StampStyle();

    public MainFrame() {
        super(App.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setLocationByPlatform(true);

        treePanel = new FileTreePanel();
        previewPanel = new PreviewPanel();
        previewPanel.setStampStyle(style);
        commentPanel = new CommentPanel();
        statusLabel = new JLabel("Keine Datei ausgewählt.");
        styleToolbar = new StyleToolbar(style, this::refreshPreviewStyle);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(commentPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(styleToolbar, BorderLayout.NORTH);
        rightPanel.add(previewPanel, BorderLayout.CENTER);
        rightPanel.add(southPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, rightPanel);
        split.setDividerLocation(320);
        setContentPane(split);

        treePanel.addFileSelectionListener(file -> {
            previewPanel.showFile(file);
            commentPanel.getSaveButton().setEnabled(previewPanel.hasImage());
            statusLabel.setText(file == null ? "Keine Datei ausgewählt." : file.getAbsolutePath());
        });
        commentPanel.addTextChangeListener(previewPanel::setOverlayText);
    }

    private void refreshPreviewStyle() {
        // Style-Objekt ist geteilt; setStampStyle stößt nur das Repaint an.
        previewPanel.setStampStyle(style);
    }

    public FileTreePanel getTreePanel() {
        return treePanel;
    }

    public PreviewPanel getPreviewPanel() {
        return previewPanel;
    }

    public CommentPanel getCommentPanel() {
        return commentPanel;
    }

    public StampStyle getStyle() {
        return style;
    }
}
