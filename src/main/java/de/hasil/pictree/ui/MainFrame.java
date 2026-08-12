package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import de.hasil.pictree.App;
import de.hasil.pictree.model.Annotation;
import de.hasil.pictree.model.EditState;
import de.hasil.pictree.model.StampStyle;
import de.hasil.pictree.service.AnnotationStore;
import de.hasil.pictree.service.AppSettings;
import de.hasil.pictree.service.BatchService;
import de.hasil.pictree.service.ExifService;
import de.hasil.pictree.service.ImageStampService;
import de.hasil.pictree.service.SaveService;
import de.hasil.pictree.util.Logging;
import de.hasil.pictree.util.UndoHistory;

/**
 * Hauptfenster: links der Datei-Baum, rechts Werkzeugleiste, Bildvorschau und
 * Kommentarfeld. Ein gemeinsames {@link StampStyle} verbindet Toolbar und
 * Vorschau (Live-Preview).
 */
public class MainFrame extends JFrame {

    private static final Logger LOG = Logging.get(MainFrame.class);

    private final FileTreePanel treePanel;
    private final PreviewPanel previewPanel;
    private final StyleToolbar styleToolbar;
    private final CommentPanel commentPanel;
    private final JLabel statusLabel;
    private final StampStyle style = new StampStyle();
    private final AppSettings settings;
    private final SaveService saveService;
    private final ExifService exifService = new ExifService();
    private final BatchService batchService;
    private final AnnotationStore annotationStore = new AnnotationStore();

    private File selectedFolder;
    /** Aktuell angezeigtes Bild, dessen Annotation bearbeitet wird (oder null). */
    private File annotatedImage;
    private final JMenu recentMenu = new JMenu("Zuletzt verwendet");

    private final UndoHistory<EditState> history = new UndoHistory<>();
    /** True, während ein Zustand wiederhergestellt wird (verhindert Neuaufzeichnung). */
    private boolean restoring;
    private JMenuItem undoItem;
    private JMenuItem redoItem;

    public MainFrame() {
        this(new AppSettings().load());
    }

    public MainFrame(AppSettings settings) {
        super(App.APP_NAME);
        this.settings = settings;
        this.saveService = new SaveService(Path.of(settings.getTargetDir()), settings.getJpegQuality());
        this.batchService = new BatchService(saveService, exifService);
        style.copyFrom(settings.getDefaultStyle());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 560));
        setSize(settings.getWindowWidth(), settings.getWindowHeight());
        setLocationByPlatform(true);

        treePanel = new FileTreePanel();
        previewPanel = new PreviewPanel();
        previewPanel.setStampStyle(style);
        commentPanel = new CommentPanel();
        statusLabel = new JLabel("Keine Datei ausgewählt.");
        styleToolbar = new StyleToolbar(style, this::refreshPreviewStyle, this::onEditCommitted);

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
        setJMenuBar(buildMenuBar());

        treePanel.addFileSelectionListener(this::onSelectionChanged);
        commentPanel.addTextChangeListener(this::onCommentChanged);
        commentPanel.getSaveButton().addActionListener(e -> onSave());
        commentPanel.getBatchButton().addActionListener(e -> onBatch());
        previewPanel.setDragCommitListener(this::onEditCommitted);
        installUndoKeyBindings();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                persistCurrentAnnotation();
                persistSettings();
            }
        });

        String last = settings.getLastFolder();
        if (!last.isBlank()) {
            SwingUtilities.invokeLater(() -> treePanel.selectPath(new File(last)));
        }
    }

    private void onSelectionChanged(File file) {
        // Zustand des bisher angezeigten Bildes sichern, bevor umgeschaltet wird.
        persistCurrentAnnotation();

        previewPanel.showFile(file);
        boolean isImage = previewPanel.hasImage();
        commentPanel.getSaveButton().setEnabled(isImage);
        selectedFolder = (file != null && file.isDirectory()) ? file : null;
        commentPanel.getBatchButton().setEnabled(selectedFolder != null);
        statusLabel.setText(file == null ? "Keine Datei ausgewählt." : file.getAbsolutePath());

        // Ordner für "Zuletzt verwendet" merken (Ordnerauswahl oder Elternordner eines Bildes).
        File folderToRemember = selectedFolder != null ? selectedFolder
                : (file != null ? file.getParentFile() : null);
        if (folderToRemember != null) {
            settings.addRecentFolder(folderToRemember.getAbsolutePath());
            updateRecentMenu();
        }

        // Beim Umschalten keine Undo-Schritte aufzeichnen; danach Historie neu starten.
        restoring = true;
        try {
            if (isImage) {
                annotatedImage = file;
                loadAnnotation(file);
            } else {
                annotatedImage = null;
                commentPanel.setText("");
            }
        } finally {
            restoring = false;
        }
        history.reset(currentEditState());
        updateUndoRedoState();
    }

    /** Lädt eine vorhandene Annotation und stellt Text, Stil und Position wieder her. */
    private void loadAnnotation(File image) {
        Optional<Annotation> loaded = annotationStore.load(image);
        if (loaded.isPresent()) {
            style.copyFrom(loaded.get().style());
            styleToolbar.syncFromStyle();
            commentPanel.setText(loaded.get().comment());
        } else {
            // Kein Sidecar: Kommentar leeren, zuletzt genutzten Stil beibehalten.
            commentPanel.setText("");
        }
        previewPanel.setStampStyle(style);
    }

    /** Speichert bzw. entfernt die Sidecar-Annotation des aktuell bearbeiteten Bildes. */
    private void persistCurrentAnnotation() {
        if (annotatedImage == null) {
            return;
        }
        try {
            String comment = commentPanel.getText();
            if (comment == null || comment.isBlank()) {
                annotationStore.delete(annotatedImage);
            } else {
                annotationStore.save(annotatedImage, comment, style);
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Annotation konnte nicht gespeichert werden", ex);
        }
    }

    /** Speichert Fenstergröße, zuletzt genutzten Ordner und Standard-Stil. */
    private void persistSettings() {
        settings.setWindowSize(getWidth(), getHeight());
        settings.setDefaultStyle(style);
        if (selectedFolder != null) {
            settings.setLastFolder(selectedFolder.getAbsolutePath());
        } else if (annotatedImage != null && annotatedImage.getParentFile() != null) {
            settings.setLastFolder(annotatedImage.getParentFile().getAbsolutePath());
        }
        settings.save();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("Datei");
        updateRecentMenu();
        file.add(recentMenu);
        bar.add(file);

        JMenu edit = new JMenu("Bearbeiten");
        undoItem = new JMenuItem("Rückgängig");
        undoItem.addActionListener(e -> undo());
        redoItem = new JMenuItem("Wiederholen");
        redoItem.addActionListener(e -> redo());
        edit.add(undoItem);
        edit.add(redoItem);
        bar.add(edit);
        updateUndoRedoState();

        JMenu view = new JMenu("Ansicht");
        JCheckBoxMenuItem darkItem = new JCheckBoxMenuItem("Dunkles Theme");
        darkItem.setSelected(AppSettings.THEME_DARK.equals(settings.getTheme()));
        darkItem.addActionListener(e -> toggleTheme(darkItem.isSelected()));
        view.add(darkItem);

        JCheckBoxMenuItem overlayItem = new JCheckBoxMenuItem("Text einblenden", true);
        overlayItem.addActionListener(e -> previewPanel.setShowOverlay(overlayItem.isSelected()));
        view.add(overlayItem);

        JCheckBoxMenuItem safeAreaItem = new JCheckBoxMenuItem("Sicherer Bereich");
        safeAreaItem.addActionListener(e -> previewPanel.setShowSafeArea(safeAreaItem.isSelected()));
        view.add(safeAreaItem);

        bar.add(view);
        return bar;
    }

    private void installUndoKeyBindings() {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask), "pictree.undo");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuMask), "pictree.redo");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask | InputEvent.SHIFT_DOWN_MASK),
                        "pictree.redo");
        root.getActionMap().put("pictree.undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });
        root.getActionMap().put("pictree.redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });
    }

    /** Aktueller bearbeitbarer Zustand (Text + Stil). */
    private EditState currentEditState() {
        return EditState.of(commentPanel.getText(), style);
    }

    /** Wird bei Abschluss einer Bearbeitung aufgerufen (Style-Commit, Drag-Ende). */
    private void onEditCommitted() {
        refreshPreviewStyle();
        recordState();
    }

    private void onCommentChanged(String text) {
        previewPanel.setOverlayText(text);
        recordState();
    }

    private void recordState() {
        if (restoring) {
            return;
        }
        history.record(currentEditState());
        updateUndoRedoState();
    }

    private void undo() {
        applyState(history.undo());
    }

    private void redo() {
        applyState(history.redo());
    }

    private void applyState(EditState state) {
        if (state == null) {
            return;
        }
        restoring = true;
        try {
            style.copyFrom(state.style());
            styleToolbar.syncFromStyle();
            commentPanel.setText(state.comment());
            previewPanel.setOverlayText(state.comment());
            previewPanel.setStampStyle(style);
        } finally {
            restoring = false;
        }
        updateUndoRedoState();
    }

    private void updateUndoRedoState() {
        if (undoItem != null) {
            undoItem.setEnabled(history.canUndo());
        }
        if (redoItem != null) {
            redoItem.setEnabled(history.canRedo());
        }
    }

    /** Baut das "Zuletzt verwendet"-Menü aus den Einstellungen neu auf. */
    private void updateRecentMenu() {
        recentMenu.removeAll();
        List<String> recent = settings.getRecentFolders();
        if (recent.isEmpty()) {
            JMenuItem empty = new JMenuItem("(keine)");
            empty.setEnabled(false);
            recentMenu.add(empty);
            return;
        }
        for (String path : recent) {
            JMenuItem item = new JMenuItem(path);
            item.addActionListener(e -> treePanel.selectPath(new File(path)));
            recentMenu.add(item);
        }
    }

    private void toggleTheme(boolean dark) {
        String theme = dark ? AppSettings.THEME_DARK : AppSettings.THEME_LIGHT;
        Themes.apply(theme);
        settings.setTheme(theme);
        SwingUtilities.updateComponentTreeUI(this);
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
