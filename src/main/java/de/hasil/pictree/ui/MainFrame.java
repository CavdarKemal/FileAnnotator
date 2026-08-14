package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
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
import javax.swing.JButton;
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
import de.hasil.pictree.model.FileTreeNode;
import de.hasil.pictree.model.ImageTypeFilter;
import de.hasil.pictree.model.LazyFileTreeModel;
import de.hasil.pictree.model.LogoOverlay;
import de.hasil.pictree.model.StampStyle;
import de.hasil.pictree.service.AnnotationStore;
import de.hasil.pictree.service.AppSettings;
import de.hasil.pictree.service.BatchService;
import de.hasil.pictree.service.ExifService;
import de.hasil.pictree.service.IccProfileService;
import de.hasil.pictree.service.ImageStampService;
import de.hasil.pictree.service.ImageSupport;
import de.hasil.pictree.service.PlaceholderResolver;
import de.hasil.pictree.service.PresetStore;
import de.hasil.pictree.service.SaveService;
import de.hasil.pictree.util.I18n;
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
    private final ThumbnailStripPanel thumbnailStrip;
    private final FileFilterControl filterControl;
    private final StyleToolbar styleToolbar;
    private final EffectsToolbar effectsToolbar;
    private final CommentPanel commentPanel;
    private final JLabel statusLabel;
    private final StampStyle style = new StampStyle();
    private final AppSettings settings;
    private final SaveService saveService;
    private final ExifService exifService = new ExifService();
    private final IccProfileService iccService = new IccProfileService();
    private final BatchService batchService;
    private final AnnotationStore annotationStore = new AnnotationStore();

    private File selectedFolder;
    /** Ordner, dessen Bilder aktuell im Thumbnail-Streifen liegen (Ziel des Stapels). */
    private File batchFolder;
    /** Aktiver Bildtyp-Filter für Baum, Streifen und Stapel. */
    private ImageTypeFilter imageFilter;
    /** Aktuell angezeigtes Bild, dessen Annotation bearbeitet wird (oder null). */
    private File annotatedImage;
    /** Datei, gegen die Text-Platzhalter der Vorschau aufgelöst werden (oder null). */
    private File overlayContextFile;
    /** Generationszähler für asynchrones Verzeichnis-Einlesen (verwirft veraltete Läufe). */
    private int folderLoadGeneration;
    private final JMenu recentMenu = new JMenu("Zuletzt verwendet");
    private final JMenu presetMenu = new JMenu("Vorlagen");
    private final PresetStore presetStore = new PresetStore();

    private LogoOverlay logoOverlay;
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

        // Beenden vollständig selbst steuern (persistieren + garantiert System.exit),
        // damit ein Fehler beim Speichern das Schließen über X nie blockiert.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(800, 560));
        setSize(settings.getWindowWidth(), settings.getWindowHeight());
        setLocationByPlatform(true);

        this.imageFilter = settings.getImageTypeFilter();
        treePanel = new FileTreePanel(new LazyFileTreeModel(FileTreeNode.createComputerRoot(imageFilter)));
        filterControl = new FileFilterControl(imageFilter, this::onFilterChanged);
        previewPanel = new PreviewPanel();
        previewPanel.setStampStyle(style);
        thumbnailStrip = new ThumbnailStripPanel();
        thumbnailStrip.setVisible(false);
        commentPanel = new CommentPanel();
        statusLabel = new JLabel(I18n.t("status.noSelection"));
        styleToolbar = new StyleToolbar(style, this::refreshPreviewStyle, this::onEditCommitted);
        effectsToolbar = new EffectsToolbar(style, this::refreshPreviewStyle, this::onEditCommitted);

        // Beenden-Button am Ende der Werkzeugleiste.
        JButton quitButton = new JButton(I18n.t("menu.quit"));
        quitButton.setToolTipText(I18n.t("menu.quit"));
        quitButton.addActionListener(e -> quit());
        styleToolbar.addSeparator();
        styleToolbar.add(quitButton);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(commentPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);

        JPanel toolbars = new JPanel();
        toolbars.setLayout(new javax.swing.BoxLayout(toolbars, javax.swing.BoxLayout.Y_AXIS));
        toolbars.add(styleToolbar);
        toolbars.add(effectsToolbar);

        // Toolbars oben, darunter der Thumbnail-Streifen – zusammen als NORTH-Bereich.
        JPanel northStack = new JPanel(new BorderLayout());
        northStack.add(toolbars, BorderLayout.NORTH);
        northStack.add(thumbnailStrip, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(northStack, BorderLayout.NORTH);
        rightPanel.add(previewPanel, BorderLayout.CENTER);
        rightPanel.add(southPanel, BorderLayout.SOUTH);

        // Links: Filter-Steuerung über dem Datei-Baum.
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(filterControl, BorderLayout.NORTH);
        leftPanel.add(treePanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(320);
        setContentPane(split);
        setJMenuBar(buildMenuBar());

        treePanel.addFileSelectionListener(this::onSelectionChanged);
        thumbnailStrip.setThumbnailClickListener(this::onThumbnailPicked);
        commentPanel.addTextChangeListener(this::onCommentChanged);
        commentPanel.getSaveButton().addActionListener(e -> onSave());
        commentPanel.getBatchButton().addActionListener(e -> onBatch());
        previewPanel.setDragCommitListener(this::onEditCommitted);
        previewPanel.setResizeCommitListener(this::onStampResized);
        installUndoKeyBindings();
        installFileDrop();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
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

        selectedFolder = (file != null && file.isDirectory()) ? file : null;
        batchFolder = selectedFolder;

        // "Zuletzt verwendet": Ordnerauswahl oder Elternordner eines Bildes.
        File recentFolder = selectedFolder != null ? selectedFolder : (file != null ? file.getParentFile() : null);
        if (recentFolder != null) {
            settings.addRecentFolder(recentFolder.getAbsolutePath());
            updateRecentMenu();
        }

        if (selectedFolder != null) {
            // Ordner-Modus: Thumbnail-Streifen + Dummy-Leinwand zum Positionieren des Stempels.
            enterFolderMode(selectedFolder);
        } else {
            // Einzeldatei/nichts: laufendes Verzeichnis-Einlesen verwerfen, Streifen ausblenden.
            folderLoadGeneration++;
            setBusy(false);
            thumbnailStrip.clear();
            setStripVisible(false);
            updateBatchButtonEnable();
            showImageForEditing(file);
        }
    }

    /**
     * Ordner-Modus: befüllt den Thumbnail-Streifen und zeigt – sofern relevante
     * Bilder vorhanden sind – ein Dummy-Bild mit dem Stempeltext, dessen Position
     * und Größe per Maus gesetzt werden können (Vorlage für den Stapel).
     */
    private void enterFolderMode(File folder) {
        final int gen = ++folderLoadGeneration;
        // Sofortige Rückmeldung: Sanduhr + Statuszeile, während das Verzeichnis gelesen wird.
        setBusy(true);
        statusLabel.setText(I18n.t("status.readingFolder") + " " + folder.getAbsolutePath());

        new SwingWorker<java.util.List<File>, Void>() {
            @Override
            protected java.util.List<File> doInBackground() {
                // Verzeichnis-I/O (listFiles + Filter) im Hintergrund, nicht auf dem EDT.
                return BatchService.listImages(folder, imageFilter);
            }

            @Override
            protected void done() {
                if (gen != folderLoadGeneration) {
                    return; // Eine neuere Auswahl hat diesen Lauf überholt.
                }
                java.util.List<File> images;
                try {
                    images = get();
                } catch (Exception ex) {
                    images = java.util.List.of();
                }
                applyFolderContents(folder, images);
                setBusy(false);
            }
        }.execute();
    }

    /** Übernimmt die im Hintergrund ermittelten Bilder eines Ordners in die UI (auf dem EDT). */
    private void applyFolderContents(File folder, java.util.List<File> images) {
        thumbnailStrip.setImages(images);
        setStripVisible(!images.isEmpty());
        updateBatchButtonEnable();

        overlayContextFile = images.isEmpty() ? null : images.get(0);
        annotatedImage = null;
        // Dummy ist kein echtes Bild -> kein Einzel-Speichern.
        commentPanel.getSaveButton().setEnabled(false);
        statusLabel.setText(folder.getAbsolutePath() + "  (" + images.size() + ")");

        restoring = true;
        try {
            if (images.isEmpty()) {
                previewPanel.showFile(folder); // zeigt "keine Bildvorschau"
            } else {
                previewPanel.showDummy();
                previewPanel.setOverlayText(PlaceholderResolver.resolve(commentPanel.getText(), overlayContextFile));
            }
        } finally {
            restoring = false;
        }
        history.reset(currentEditState());
        updateUndoRedoState();
    }

    /** Schaltet die Warteanzeige (Sanduhr über der Glass-Pane; blockiert Eingaben währenddessen). */
    private void setBusy(boolean busy) {
        Component glass = getGlassPane();
        glass.setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        glass.setVisible(busy);
    }

    /**
     * Wechselt das angezeigte Bild und stellt dessen Annotation wieder her –
     * ohne Baum-Selektion oder Thumbnail-Streifen zu verändern.
     */
    private void showImageForEditing(File file) {
        previewPanel.showFile(file);
        boolean isImage = previewPanel.hasImage();
        overlayContextFile = isImage ? file : null;
        commentPanel.getSaveButton().setEnabled(isImage);
        statusLabel.setText(file == null ? I18n.t("status.noSelection") : file.getAbsolutePath());

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

    /**
     * Klick auf ein Thumbnail (im Ordner-Modus): zeigt den aktuellen Stempel auf
     * dem echten Bild als Batch-Vorschau – ohne Batch-Text/-Stil oder die Auswahl
     * im Streifen zu verändern.
     */
    private void onThumbnailPicked(File file) {
        previewPanel.showFile(file);
        overlayContextFile = file;
        previewPanel.setOverlayText(PlaceholderResolver.resolve(commentPanel.getText(), file));
        commentPanel.getSaveButton().setEnabled(previewPanel.hasImage());
        statusLabel.setText(file.getAbsolutePath());
    }

    /** Blendet den Thumbnail-Streifen ein/aus und ordnet das Layout neu an. */
    private void setStripVisible(boolean visible) {
        if (thumbnailStrip.isVisible() != visible) {
            thumbnailStrip.setVisible(visible);
            java.awt.Container parent = thumbnailStrip.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        }
    }

    /** Der Stapel-Button ist aktiv, sobald der Streifen mindestens ein Bild zeigt. */
    private void updateBatchButtonEnable() {
        commentPanel.getBatchButton().setEnabled(batchFolder != null && thumbnailStrip.getImageCount() > 0);
    }

    /** Größenänderung des Stempels per Maus abgeschlossen: Toolbar angleichen + Undo-Schritt. */
    private void onStampResized() {
        refreshPreviewStyle();
        restoring = true;
        try {
            syncToolbars();
        } finally {
            restoring = false;
        }
        recordState();
    }

    /** Filteränderung: persistieren, Baum neu aufbauen (Selektion erhalten), Ansicht aktualisieren. */
    private void onFilterChanged(ImageTypeFilter filter) {
        imageFilter = filter;
        settings.setImageTypeFilter(filter);
        treePanel.rebuildPreservingSelection(new LazyFileTreeModel(FileTreeNode.createComputerRoot(filter)));
        if (selectedFolder != null) {
            enterFolderMode(selectedFolder);
        } else {
            thumbnailStrip.clear();
            setStripVisible(false);
            updateBatchButtonEnable();
        }
    }

    /** Lädt eine vorhandene Annotation und stellt Text, Stil und Position wieder her. */
    private void loadAnnotation(File image) {
        Optional<Annotation> loaded = annotationStore.load(image);
        if (loaded.isPresent()) {
            style.copyFrom(loaded.get().style());
            syncToolbars();
            commentPanel.setText(loaded.get().comment());
        } else {
            // Kein Sidecar: Kommentar leeren, zuletzt genutzten Stil beibehalten.
            commentPanel.setText("");
        }
        previewPanel.setStampStyle(style);
    }

    /** Speichert bzw. entfernt die Sidecar-Annotation des aktuell bearbeiteten Bildes. */
    /** Persistiert (fehlerrobust) und beendet die Anwendung garantiert. */
    private void quit() {
        try {
            persistCurrentAnnotation();
            persistSettings();
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Beim Beenden ist ein Fehler aufgetreten", ex);
        }
        dispose();
        System.exit(0);
    }

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

        JMenu file = new JMenu(I18n.t("menu.file"));
        recentMenu.setText(I18n.t("menu.recent"));
        updateRecentMenu();
        file.add(recentMenu);
        file.addSeparator();
        JMenuItem quitItem = new JMenuItem(I18n.t("menu.quit"));
        quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        quitItem.addActionListener(e -> quit());
        file.add(quitItem);
        bar.add(file);

        JMenu edit = new JMenu(I18n.t("menu.edit"));
        undoItem = new JMenuItem(I18n.t("menu.undo"));
        undoItem.addActionListener(e -> undo());
        redoItem = new JMenuItem(I18n.t("menu.redo"));
        redoItem.addActionListener(e -> redo());
        edit.add(undoItem);
        edit.add(redoItem);
        bar.add(edit);
        updateUndoRedoState();

        JMenu view = new JMenu(I18n.t("menu.view"));
        JCheckBoxMenuItem darkItem = new JCheckBoxMenuItem(I18n.t("menu.view.dark"));
        darkItem.setSelected(AppSettings.THEME_DARK.equals(settings.getTheme()));
        darkItem.addActionListener(e -> toggleTheme(darkItem.isSelected()));
        view.add(darkItem);

        JCheckBoxMenuItem overlayItem = new JCheckBoxMenuItem(I18n.t("menu.view.overlay"), true);
        overlayItem.addActionListener(e -> previewPanel.setShowOverlay(overlayItem.isSelected()));
        view.add(overlayItem);

        JCheckBoxMenuItem safeAreaItem = new JCheckBoxMenuItem(I18n.t("menu.view.safearea"));
        safeAreaItem.addActionListener(e -> previewPanel.setShowSafeArea(safeAreaItem.isSelected()));
        view.add(safeAreaItem);

        JMenu language = new JMenu(I18n.t("menu.view.language"));
        addLanguageItem(language, "de", I18n.t("language.de"));
        addLanguageItem(language, "en", I18n.t("language.en"));
        view.add(language);

        bar.add(view);

        presetMenu.setText(I18n.t("menu.presets"));
        updatePresetMenu();
        bar.add(presetMenu);

        bar.add(buildLogoMenu());
        return bar;
    }

    private JMenu buildLogoMenu() {
        JMenu logo = new JMenu(I18n.t("menu.logo"));
        JMenuItem choose = new JMenuItem(I18n.t("menu.logo.choose"));
        choose.addActionListener(e -> onChooseLogo());
        logo.add(choose);
        JMenuItem remove = new JMenuItem(I18n.t("menu.logo.remove"));
        remove.addActionListener(e -> {
            logoOverlay = null;
            previewPanel.setLogoOverlay(null);
        });
        logo.add(remove);

        JMenu position = new JMenu(I18n.t("menu.logo.position"));
        addLogoPosition(position, "Oben links", 0.15, 0.15);
        addLogoPosition(position, "Oben rechts", 0.85, 0.15);
        addLogoPosition(position, "Mitte", 0.5, 0.5);
        addLogoPosition(position, "Unten links", 0.15, 0.85);
        addLogoPosition(position, "Unten rechts", 0.85, 0.85);
        logo.add(position);

        JMenu opacity = new JMenu(I18n.t("menu.logo.opacity"));
        for (int pct : new int[] {25, 50, 75, 100}) {
            JMenuItem item = new JMenuItem(pct + " %");
            float value = pct / 100f;
            item.addActionListener(e -> {
                if (logoOverlay != null) {
                    logoOverlay.setOpacity(value);
                    previewPanel.repaint();
                }
            });
            opacity.add(item);
        }
        logo.add(opacity);
        return logo;
    }

    private void addLogoPosition(JMenu menu, String label, double rx, double ry) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            if (logoOverlay != null) {
                logoOverlay.setRelX(rx);
                logoOverlay.setRelY(ry);
                previewPanel.repaint();
            }
        });
        menu.add(item);
    }

    private void onChooseLogo() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Logo-/Wasserzeichen-Bild wählen");
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        BufferedImage img = ImageSupport.load(file);
        if (img == null) {
            JOptionPane.showMessageDialog(this, "Bild konnte nicht geladen werden:\n" + file,
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        logoOverlay = new LogoOverlay(file, img);
        previewPanel.setLogoOverlay(logoOverlay);
        statusLabel.setText("Logo gesetzt: " + file.getName());
    }

    /** Baut das "Vorlagen"-Menü neu auf (Speichern + Liste + Löschen). */
    private void updatePresetMenu() {
        presetMenu.removeAll();
        JMenuItem saveAs = new JMenuItem(I18n.t("menu.presets.saveAs"));
        saveAs.addActionListener(e -> onSavePreset());
        presetMenu.add(saveAs);

        List<String> names = presetStore.names();
        if (!names.isEmpty()) {
            presetMenu.addSeparator();
            for (String name : names) {
                JMenuItem item = new JMenuItem(name);
                item.addActionListener(e -> applyPreset(name));
                presetMenu.add(item);
            }
            JMenu deleteMenu = new JMenu(I18n.t("menu.presets.delete"));
            for (String name : names) {
                JMenuItem del = new JMenuItem(name);
                del.addActionListener(e -> {
                    presetStore.delete(name);
                    updatePresetMenu();
                });
                deleteMenu.add(del);
            }
            presetMenu.addSeparator();
            presetMenu.add(deleteMenu);
        }
    }

    private void addLanguageItem(JMenu menu, String lang, String label) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, settings.getLanguage().equals(lang));
        item.addActionListener(e -> {
            settings.setLanguage(lang);
            settings.save();
            JOptionPane.showMessageDialog(this, I18n.t("language.restart"),
                    I18n.t("menu.view.language"), JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(item);
    }

    private void onSavePreset() {
        String name = JOptionPane.showInputDialog(this, "Name der Vorlage:", "Vorlage speichern",
                JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.isBlank()) {
            presetStore.save(name.trim(), style.copy());
            updatePresetMenu();
            statusLabel.setText("Vorlage gespeichert: " + name.trim());
        }
    }

    private void applyPreset(String name) {
        presetStore.load(name).ifPresent(preset -> {
            style.copyFrom(preset);
            syncToolbars();
            previewPanel.setStampStyle(style);
            recordState();
            statusLabel.setText("Vorlage angewendet: " + name);
        });
    }

    /** Ermöglicht das Ziehen von Dateien/Ordnern aus dem Explorer ins Fenster. */
    private void installFileDrop() {
        new java.awt.dnd.DropTarget(this, java.awt.dnd.DnDConstants.ACTION_COPY, new java.awt.dnd.DropTargetAdapter() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void drop(java.awt.dnd.DropTargetDropEvent event) {
                        try {
                            event.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                            java.util.List<File> files = (java.util.List<File>) event.getTransferable()
                                    .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                            File target = pickDropTarget(files);
                            if (target != null) {
                                treePanel.selectPath(target);
                            }
                            event.dropComplete(true);
                        } catch (Exception ex) {
                            LOG.log(Level.WARNING, "Drop fehlgeschlagen", ex);
                            event.dropComplete(false);
                        }
                    }
                });
    }

    /** Wählt das Ziel eines Drops: erste existierende Datei/erster Ordner, sonst {@code null}. */
    static File pickDropTarget(java.util.List<File> files) {
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (f != null && f.exists()) {
                return f;
            }
        }
        return null;
    }

    private void installUndoKeyBindings() {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask), "pictree.undo");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuMask), "pictree.redo");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask | InputEvent.SHIFT_DOWN_MASK), "pictree.redo");
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
        // Platzhalter (z. B. {datum}, {dateiname}) für die Vorschau auflösen.
        previewPanel.setOverlayText(PlaceholderResolver.resolve(text, overlayContextFile));
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
            syncToolbars();
            commentPanel.setText(state.comment());
            previewPanel.setOverlayText(PlaceholderResolver.resolve(state.comment(), overlayContextFile));
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
            JMenuItem empty = new JMenuItem(I18n.t("menu.recent.empty"));
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

    /** Aktualisiert beide Werkzeugleisten aus dem aktuellen Stil. */
    private void syncToolbars() {
        styleToolbar.syncFromStyle();
        effectsToolbar.syncFromStyle();
    }

    private void onSave() {
        BufferedImage src = previewPanel.getImage();
        File original = previewPanel.getCurrentFile();
        if (src == null || original == null) {
            return;
        }
        try {
            String resolved = PlaceholderResolver.resolve(commentPanel.getText(), original);
            BufferedImage stamped = ImageStampService.renderStamp(src, resolved, style, logoOverlay);
            // Neben dem Original im Quellordner speichern (kollisionsfreier neuer Name);
            // das Original bleibt unberührt.
            File targetDir = original.getParentFile();
            SaveService out = targetDir != null ? new SaveService(targetDir.toPath(), saveService.getQuality())
                    : saveService;
            File saved = out.save(stamped, original.getName());
            boolean exifCopied = exifService.copyExif(original, saved);
            iccService.copyIccProfile(original, saved);
            statusLabel.setText("Gespeichert: " + saved.getAbsolutePath() + (exifCopied ? " (EXIF übernommen)" : ""));
            JOptionPane.showMessageDialog(this, "Bild gespeichert:\n" + saved.getAbsolutePath()
                            + (exifCopied ? "\nEXIF-Daten wurden übernommen." : ""),
                    "Gespeichert", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Speichern fehlgeschlagen:\n" + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onBatch() {
        if (batchFolder == null) {
            JOptionPane.showMessageDialog(this, I18n.t("batch.noFolder"),
                    "Stapelverarbeitung", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final java.util.List<File> selected = thumbnailStrip.getSelectedImages();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("batch.noSelection"),
                    "Stapelverarbeitung", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Zielordner aus dem Stempeltext ableiten (Einzel-Save bleibt unberührt).
        final File outputDir = new File(batchFolder, BatchService.sanitizeFolderName(commentPanel.getText()));
        int choice = JOptionPane.showConfirmDialog(this,
                selected.size() + " ausgewählte(s) Bild(er)\nmit demselben Stempel versehen und speichern nach:\n"
                        + outputDir.getAbsolutePath() + " ?", "Stapelverarbeitung", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        final String text = commentPanel.getText();
        final StampStyle snapshot = style.copy();
        final LogoOverlay logoSnapshot = logoOverlay;
        commentPanel.getBatchButton().setEnabled(false);

        new SwingWorker<BatchService.BatchResult, String>() {
            @Override
            protected BatchService.BatchResult doInBackground() {
                return batchService.processFiles(selected, outputDir, text, snapshot, logoSnapshot,
                        (done, total, current, saved) ->
                        publish("Stapel: " + done + "/" + total + " – " + current.getName()));
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                updateBatchButtonEnable();
                try {
                    BatchService.BatchResult result = get();
                    statusLabel.setText("Stapel fertig: " + result.saved().size() + " gespeichert, "
                            + result.failed().size() + " fehlgeschlagen.");
                    showBatchResult(result);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Stapelverarbeitung fehlgeschlagen:\n" + ex.getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Zeigt das Batch-Ergebnis; bei Fehlern mit scrollbarer Detailliste. */
    private void showBatchResult(BatchService.BatchResult result) {
        String header = "Stapelverarbeitung abgeschlossen.\n" + result.saved().size() + " Bild(er) gespeichert nach:\n"
                + result.outputDir() + "\n" + result.failed().size() + " fehlgeschlagen.";
        if (result.failed().isEmpty()) {
            JOptionPane.showMessageDialog(this, header, "Stapelverarbeitung", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder details = new StringBuilder(header).append("\n\nFehler:\n");
        for (BatchService.Failure f : result.failed()) {
            details.append("• ").append(f.file().getName()).append(": ").append(f.reason()).append('\n');
        }
        javax.swing.JTextArea area = new javax.swing.JTextArea(details.toString(), 14, 48);
        area.setEditable(false);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(area),
                "Stapelverarbeitung – Fehlerreport", JOptionPane.WARNING_MESSAGE);
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
