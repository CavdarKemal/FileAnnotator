package de.hasil.pictree.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import de.hasil.pictree.service.ImageSupport;
import de.hasil.pictree.service.Images;
import de.hasil.pictree.util.I18n;

/**
 * Horizontaler, scrollbarer Streifen mit Thumbnails der Bilder eines Ordners.
 * Jedes Thumbnail besitzt eine Checkbox; eine „Alle/Keine"-Checkbox schaltet
 * die gesamte Auswahl. Thumbnails werden im Hintergrund geladen (EDT bleibt
 * frei); ein Generations-Zähler verwirft veraltete Ladeergebnisse bei
 * Ordnerwechsel. Die Auswahl steuert, welche Bilder die Stapelverarbeitung
 * stempelt (siehe {@code MainFrame.onBatch}).
 */
public class ThumbnailStripPanel extends JPanel {

    /** Kantenlänge der Thumbnails in Pixeln. */
    static final int THUMB_SIZE = 120;
    private static final int STRIP_HEIGHT = 172;

    private final JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final JCheckBox allBox = new JCheckBox(I18n.t("thumbnails.all"), true);
    private final List<Cell> cells = new ArrayList<>();
    private final transient ImageIcon placeholder = createPlaceholder();

    private transient Consumer<File> clickListener;
    private transient Runnable selectionListener;
    private transient SwingWorker<Void, IconChunk> worker;
    private int generation;

    public ThumbnailStripPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        allBox.addActionListener(e -> {
            boolean sel = allBox.isSelected();
            for (Cell c : cells) {
                c.check.setSelected(sel);
            }
            fireSelectionChanged();
        });
        top.add(allBox);
        add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(strip,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getHorizontalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        setPreferredSize(new Dimension(0, STRIP_HEIGHT));
    }

    /** Baut den Streifen für die übergebenen Bilder neu auf und lädt Thumbnails asynchron. */
    public void setImages(List<File> images) {
        generation++;
        cancelWorker();

        cells.clear();
        strip.removeAll();
        for (File f : images) {
            Cell cell = new Cell(f);
            cells.add(cell);
            strip.add(cell.panel);
        }
        allBox.setSelected(!cells.isEmpty());
        strip.revalidate();
        strip.repaint();
        fireSelectionChanged();

        if (!images.isEmpty()) {
            startLoading(generation, new ArrayList<>(images));
        }
    }

    /** Leert den Streifen. */
    public void clear() {
        setImages(List.of());
    }

    private void startLoading(int gen, List<File> images) {
        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (int i = 0; i < images.size(); i++) {
                    if (isCancelled() || gen != generation) {
                        return null;
                    }
                    BufferedImage full = ImageSupport.load(images.get(i));
                    if (full != null) {
                        BufferedImage thumb = Images.downscaleToMax(full, THUMB_SIZE);
                        publish(new IconChunk(gen, i, new ImageIcon(thumb)));
                    }
                }
                return null;
            }

            @Override
            protected void process(List<IconChunk> chunks) {
                for (IconChunk chunk : chunks) {
                    if (chunk.gen != generation || chunk.index >= cells.size()) {
                        continue;
                    }
                    cells.get(chunk.index).thumb.setIcon(chunk.icon);
                }
            }
        };
        worker.execute();
    }

    private void cancelWorker() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        worker = null;
    }

    /** Die aktuell angehakten Bilder in Anzeige-Reihenfolge. */
    public List<File> getSelectedImages() {
        List<File> result = new ArrayList<>();
        for (Cell c : cells) {
            if (c.check.isSelected()) {
                result.add(c.file);
            }
        }
        return result;
    }

    /** Anzahl aktuell angezeigter Thumbnails (unabhängig von der Auswahl). */
    public int getImageCount() {
        return cells.size();
    }

    public void setThumbnailClickListener(Consumer<File> listener) {
        this.clickListener = listener;
    }

    /** Callback bei jeder Änderung der Auswahl (für z. B. Button-Enable). */
    public void setSelectionListener(Runnable listener) {
        this.selectionListener = listener;
    }

    private void fireSelectionChanged() {
        if (selectionListener != null) {
            selectionListener.run();
        }
    }

    private void onCellToggled() {
        // "Alle/Keine" spiegelt wider, ob wirklich alle angehakt sind.
        boolean allSelected = !cells.isEmpty();
        for (Cell c : cells) {
            if (!c.check.isSelected()) {
                allSelected = false;
                break;
            }
        }
        allBox.setSelected(allSelected);
        fireSelectionChanged();
    }

    private static ImageIcon createPlaceholder() {
        BufferedImage img = new BufferedImage(THUMB_SIZE, THUMB_SIZE, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(new Color(60, 60, 60));
        g.fillRect(0, 0, THUMB_SIZE, THUMB_SIZE);
        g.dispose();
        return new ImageIcon(img);
    }

    /** Eine Zelle: Checkbox mit Dateiname oben, Thumbnail darunter. */
    private final class Cell {
        final File file;
        final JCheckBox check;
        final JLabel thumb;
        final JPanel panel;

        Cell(File file) {
            this.file = file;
            this.check = new JCheckBox(shorten(file.getName()), true);
            this.check.setToolTipText(file.getName());
            this.check.addActionListener(e -> onCellToggled());

            this.thumb = new JLabel(placeholder);
            this.thumb.setHorizontalAlignment(SwingConstants.CENTER);
            this.thumb.setPreferredSize(new Dimension(THUMB_SIZE, THUMB_SIZE));
            this.thumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            this.thumb.setToolTipText(file.getName());
            this.thumb.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (clickListener != null) {
                        clickListener.accept(file);
                    }
                }
            });

            this.panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            check.setAlignmentX(CENTER_ALIGNMENT);
            thumb.setAlignmentX(CENTER_ALIGNMENT);
            panel.add(check);
            panel.add(thumb);
        }

        private String shorten(String name) {
            return name.length() <= 16 ? name : name.substring(0, 13) + "…";
        }
    }

    /** Fortschritts-Häppchen: geladenes Icon für eine Zelle einer bestimmten Generation. */
    private static final class IconChunk {
        final int gen;
        final int index;
        final ImageIcon icon;

        IconChunk(int gen, int index, ImageIcon icon) {
            this.gen = gen;
            this.index = index;
            this.icon = icon;
        }
    }
}
