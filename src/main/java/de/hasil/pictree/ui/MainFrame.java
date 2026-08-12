package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import de.hasil.pictree.App;
import de.hasil.pictree.model.StampStyle;
import de.hasil.pictree.service.BatchService;
import de.hasil.pictree.service.ExifService;
import de.hasil.pictree.service.ImageStampService;
import de.hasil.pictree.service.SaveService;

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
    private final SaveService saveService = new SaveService();
    private final ExifService exifService = new ExifService();
    private final BatchService batchService = new BatchService(saveService, exifService);

    private File selectedFolder;

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
            selectedFolder = (file != null && file.isDirectory()) ? file : null;
            commentPanel.getBatchButton().setEnabled(selectedFolder != null);
            statusLabel.setText(file == null ? "Keine Datei ausgewählt." : file.getAbsolutePath());
        });
        commentPanel.addTextChangeListener(previewPanel::setOverlayText);
        commentPanel.getSaveButton().addActionListener(e -> onSave());
        commentPanel.getBatchButton().addActionListener(e -> onBatch());
    }

    private void refreshPreviewStyle() {
        // Style-Objekt ist geteilt; setStampStyle stößt nur das Repaint an.
        previewPanel.setStampStyle(style);
    }

    private void onSave() {
        BufferedImage src = previewPanel.getImage();
        File original = previewPanel.getCurrentFile();
        if (src == null || original == null) {
            return;
        }
        try {
            BufferedImage stamped = ImageStampService.renderStamp(src, commentPanel.getText(), style);
            File saved = saveService.save(stamped, original.getName());
            boolean exifCopied = exifService.copyExif(original, saved);
            statusLabel.setText("Gespeichert: " + saved.getAbsolutePath()
                    + (exifCopied ? " (EXIF übernommen)" : ""));
            JOptionPane.showMessageDialog(this,
                    "Bild gespeichert:\n" + saved.getAbsolutePath()
                            + (exifCopied ? "\nEXIF-Daten wurden übernommen." : ""),
                    "Gespeichert", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Speichern fehlgeschlagen:\n" + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onBatch() {
        if (selectedFolder == null) {
            return;
        }
        int count = BatchService.listImages(selectedFolder).size();
        if (count == 0) {
            JOptionPane.showMessageDialog(this,
                    "Im Ordner wurden keine Bilder gefunden:\n" + selectedFolder.getAbsolutePath(),
                    "Stapelverarbeitung", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                count + " Bild(er) in\n" + selectedFolder.getAbsolutePath()
                        + "\nmit demselben Stempel versehen und speichern?",
                "Stapelverarbeitung", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        final File folder = selectedFolder;
        final String text = commentPanel.getText();
        final StampStyle snapshot = style.copy();
        commentPanel.getBatchButton().setEnabled(false);

        new SwingWorker<BatchService.BatchResult, String>() {
            @Override
            protected BatchService.BatchResult doInBackground() {
                return batchService.processFolder(folder, text, snapshot, (done, total, current, saved) ->
                        publish("Stapel: " + done + "/" + total + " – " + current.getName()));
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                commentPanel.getBatchButton().setEnabled(selectedFolder != null);
                try {
                    BatchService.BatchResult result = get();
                    statusLabel.setText("Stapel fertig: " + result.saved().size() + " gespeichert, "
                            + result.failed().size() + " fehlgeschlagen.");
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Stapelverarbeitung abgeschlossen.\n"
                                    + result.saved().size() + " Bild(er) gespeichert nach:\n"
                                    + saveService.getTargetDir() + "\n"
                                    + result.failed().size() + " fehlgeschlagen.",
                            "Stapelverarbeitung", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Stapelverarbeitung fehlgeschlagen:\n" + ex.getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
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
