package de.hasil.pictree.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JPanel;

import de.hasil.pictree.model.StampStyle;
import de.hasil.pictree.service.ImageSupport;
import de.hasil.pictree.service.TextStampRenderer;

/**
 * Vorschau-Fläche: zeigt das selektierte Bild seitenverhältnistreu eingepasst
 * und darüber den Text-Stempel (WYSIWYG). Nicht-Bilder werden mit Hinweis
 * angezeigt; die Stempel-Funktion ist dann inaktiv.
 */
public class PreviewPanel extends JPanel {

    private BufferedImage image;
    private File currentFile;
    private String overlayText = "";
    private StampStyle style = new StampStyle();

    /** Zuletzt gezeichnetes Bildrechteck (Panel-Koordinaten) – Basis für Drag. */
    private Rectangle lastImageRect = new Rectangle(0, 0, 0, 0);

    /** Zuletzt gezeichnete Text-Bounding-Box (Panel-Koordinaten) für Treffer-Test. */
    private Rectangle lastTextRect;

    private static final int DRAG_PADDING = 8;
    private final TextDragModel drag = new TextDragModel();
    /** Wird nach Abschluss eines Ziehvorgangs aufgerufen (z. B. Undo-Schritt aufzeichnen). */
    private Runnable dragCommitListener = () -> { };

    public PreviewPanel() {
        setBackground(new Color(40, 40, 40));
        installDragHandlers();
    }

    private void installDragHandlers() {
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (hasImage() && TextDragModel.hitTest(e.getPoint(), lastTextRect, DRAG_PADDING)) {
                    drag.begin(e.getPoint(), style.getRelX(), style.getRelY(), lastImageRect);
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (drag.isDragging()) {
                    Point2D.Double rel = drag.update(e.getPoint(), lastImageRect);
                    style.setRelX(rel.x);
                    style.setRelY(rel.y);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean wasDragging = drag.isDragging();
                drag.end();
                updateCursor(e.getPoint());
                if (wasDragging) {
                    dragCommitListener.run();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getPoint());
            }
        };
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    private void updateCursor(java.awt.Point p) {
        boolean overText = hasImage() && TextDragModel.hitTest(p, lastTextRect, DRAG_PADDING);
        setCursor(Cursor.getPredefinedCursor(overText ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Selektiert eine Datei; lädt sie als Bild oder schaltet in den Nicht-Bild-Zustand. */
    public void showFile(File file) {
        this.currentFile = file;
        this.image = ImageSupport.load(file);
        repaint();
    }

    public void setOverlayText(String text) {
        this.overlayText = text == null ? "" : text;
        repaint();
    }

    /** Registriert einen Callback, der nach Abschluss eines Ziehvorgangs feuert. */
    public void setDragCommitListener(Runnable listener) {
        this.dragCommitListener = listener == null ? () -> { } : listener;
    }

    public void setStampStyle(StampStyle style) {
        this.style = style == null ? new StampStyle() : style;
        repaint();
    }

    public StampStyle getStampStyle() {
        return style;
    }

    /** True, wenn aktuell ein Bild angezeigt wird (Stempeln möglich). */
    public boolean hasImage() {
        return image != null;
    }

    public BufferedImage getImage() {
        return image;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public Rectangle getLastImageRect() {
        return new Rectangle(lastImageRect);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (image == null) {
                paintPlaceholder(g2);
                lastImageRect = new Rectangle(0, 0, 0, 0);
                lastTextRect = null;
                return;
            }

            Rectangle fit = PreviewGeometry.computeFitRect(
                    image.getWidth(), image.getHeight(), getWidth(), getHeight());
            lastImageRect = fit;
            g2.drawImage(image, fit.x, fit.y, fit.width, fit.height, null);
            lastTextRect = TextStampRenderer.drawStamp(g2, overlayText, style, fit);
        } finally {
            g2.dispose();
        }
    }

    private void paintPlaceholder(Graphics2D g2) {
        g2.setColor(Color.LIGHT_GRAY);
        String msg = currentFile == null
                ? "Keine Datei ausgewählt."
                : "Keine Bildvorschau: " + currentFile.getName();
        int tw = g2.getFontMetrics().stringWidth(msg);
        g2.drawString(msg, (getWidth() - tw) / 2, getHeight() / 2);
    }
}
