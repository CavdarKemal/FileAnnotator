package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import de.hasil.pictree.App;

/**
 * Hauptfenster: links der Datei-Baum, rechts die Bildvorschau (weitere Werkzeuge
 * folgen in den nächsten Schritten).
 */
public class MainFrame extends JFrame {

    private final FileTreePanel treePanel;
    private final PreviewPanel previewPanel;
    private final JLabel statusLabel;

    public MainFrame() {
        super(App.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setLocationByPlatform(true);

        treePanel = new FileTreePanel();
        previewPanel = new PreviewPanel();
        statusLabel = new JLabel("Keine Datei ausgewählt.");

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(previewPanel, BorderLayout.CENTER);
        rightPanel.add(statusLabel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, rightPanel);
        split.setDividerLocation(320);
        setContentPane(split);

        treePanel.addFileSelectionListener(file -> {
            previewPanel.showFile(file);
            statusLabel.setText(file == null ? "Keine Datei ausgewählt." : file.getAbsolutePath());
        });
    }

    public FileTreePanel getTreePanel() {
        return treePanel;
    }

    public PreviewPanel getPreviewPanel() {
        return previewPanel;
    }
}
