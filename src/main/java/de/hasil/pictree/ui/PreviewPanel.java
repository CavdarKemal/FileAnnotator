package de.hasil.pictree.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

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

    /** Aktive Einrast-Hilfslinien (während des Ziehens). */
    private boolean guideX;
    private boolean guideY;

    /** Zoom (1.0 = eingepasst) und Pan-Versatz in Panel-Pixeln. */
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 8.0;
    private double zoom = 1.0;
    private int panX;
    private int panY;
    private boolean panning;
    private int panStartX;
    private int panStartY;
    private int panOrigX;
    private int panOrigY;

    public PreviewPanel() {
        setBackground(new Color(40, 40, 40));
        setFocusable(true);
        installDragHandlers();
        installKeyBindings();
    }

    /** Basis-Einpassrechteck (Zoom 1, kein Pan). */
    private Rectangle baseFitRect() {
        if (image == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        return PreviewGeometry.computeFitRect(image.getWidth(), image.getHeight(), getWidth(), getHeight());
    }

    /** Aktuell dargestelltes Bildrechteck (mit Zoom und Pan). */
    private Rectangle displayRect() {
        return PreviewGeometry.zoomedRect(baseFitRect(), zoom, panX, panY);
    }

    private void resetView() {
        zoom = 1.0;
        panX = 0;
        panY = 0;
    }

    private void installDragHandlers() {
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                // Mittlere/rechte Maustaste (oder Strg+links) verschiebt die Ansicht (Pan).
                boolean panButton = e.getButton() == MouseEvent.BUTTON2
                        || e.getButton() == MouseEvent.BUTTON3
                        || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown());
                if (panButton && hasImage() && zoom > MIN_ZOOM) {
                    panning = true;
                    panStartX = e.getX();
                    panStartY = e.getY();
                    panOrigX = panX;
                    panOrigY = panY;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }
                if (hasImage() && TextDragModel.hitTest(e.getPoint(), lastTextRect, DRAG_PADDING)) {
                    drag.begin(e.getPoint(), style.getRelX(), style.getRelY(), lastImageRect);
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (panning) {
                    panX = panOrigX + (e.getX() - panStartX);
                    panY = panOrigY + (e.getY() - panStartY);
                    repaint();
                    return;
                }
                if (drag.isDragging()) {
                    Point2D.Double rel = drag.update(e.getPoint(), lastImageRect);
                    Snapping.Snap sx = Snapping.snap(rel.x);
                    Snapping.Snap sy = Snapping.snap(rel.y);
                    guideX = sx.snapped();
                    guideY = sy.snapped();
                    style.setRelX(sx.value());
                    style.setRelY(sy.value());
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (panning) {
                    panning = false;
                    updateCursor(e.getPoint());
                    return;
                }
                boolean wasDragging = drag.isDragging();
                drag.end();
                guideX = false;
                guideY = false;
                updateCursor(e.getPoint());
                repaint();
                if (wasDragging) {
                    dragCommitListener.run();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getPoint());
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (hasImage()) {
                    zoomAt(e.getPoint(), e.getPreciseWheelRotation() < 0 ? 1.15 : 1 / 1.15);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    resetView(); // Doppelklick: Ansicht zurücksetzen (einpassen)
                    repaint();
                }
            }
        };
        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
    }

    /** Zoomt um den Faktor {@code factor} und hält den Punkt unter dem Cursor stabil. */
    private void zoomAt(Point cursor, double factor) {
        Rectangle before = displayRect();
        double fx = before.width > 0 ? (cursor.x - before.x) / (double) before.width : 0.5;
        double fy = before.height > 0 ? (cursor.y - before.y) / (double) before.height : 0.5;

        double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        if (newZoom == zoom) {
            return;
        }
        zoom = newZoom;
        if (zoom <= MIN_ZOOM + 1e-9) {
            panX = 0;
            panY = 0;
        } else {
            int[] pan = PreviewGeometry.panForZoomAbout(baseFitRect(), zoom, cursor.x, cursor.y, fx, fy);
            panX = pan[0];
            panY = pan[1];
        }
        repaint();
    }

    private void updateCursor(Point p) {
        boolean overText = hasImage() && TextDragModel.hitTest(p, lastTextRect, DRAG_PADDING);
        setCursor(Cursor.getPredefinedCursor(overText ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void installKeyBindings() {
        bindNudge("nudge.left", KeyEvent.VK_LEFT, 0, -1, 0, 1);
        bindNudge("nudge.right", KeyEvent.VK_RIGHT, 0, 1, 0, 1);
        bindNudge("nudge.up", KeyEvent.VK_UP, 0, 0, -1, 1);
        bindNudge("nudge.down", KeyEvent.VK_DOWN, 0, 0, 1, 1);
        bindNudge("nudge.leftBig", KeyEvent.VK_LEFT, java.awt.event.InputEvent.SHIFT_DOWN_MASK, -1, 0, 10);
        bindNudge("nudge.rightBig", KeyEvent.VK_RIGHT, java.awt.event.InputEvent.SHIFT_DOWN_MASK, 1, 0, 10);
        bindNudge("nudge.upBig", KeyEvent.VK_UP, java.awt.event.InputEvent.SHIFT_DOWN_MASK, 0, -1, 10);
        bindNudge("nudge.downBig", KeyEvent.VK_DOWN, java.awt.event.InputEvent.SHIFT_DOWN_MASK, 0, 1, 10);
    }

    private void bindNudge(String name, int keyCode, int modifiers, int dx, int dy, int stepPx) {
        getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(keyCode, modifiers), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nudge(dx, dy, stepPx);
            }
        });
    }

    /** Verschiebt den Textanker um {@code stepPx} Vorschau-Pixel in Richtung (dx,dy). */
    private void nudge(int dx, int dy, int stepPx) {
        if (!hasImage() || lastImageRect.width <= 0 || lastImageRect.height <= 0) {
            return;
        }
        double newX = style.getRelX() + (double) (dx * stepPx) / lastImageRect.width;
        double newY = style.getRelY() + (double) (dy * stepPx) / lastImageRect.height;
        style.setRelX(newX);
        style.setRelY(newY);
        repaint();
        dragCommitListener.run(); // jede Tastatur-Verschiebung ist ein Undo-Schritt
    }

    /** Selektiert eine Datei; lädt sie als Bild oder schaltet in den Nicht-Bild-Zustand. */
    public void showFile(File file) {
        this.currentFile = file;
        this.image = ImageSupport.load(file);
        resetView();
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

            Rectangle fit = displayRect();
            lastImageRect = fit;
            g2.drawImage(image, fit.x, fit.y, fit.width, fit.height, null);
            lastTextRect = TextStampRenderer.drawStamp(g2, overlayText, style, fit);
            paintGuides(g2, fit);
        } finally {
            g2.dispose();
        }
    }

    /** Zeichnet Einrast-Hilfslinien am aktuellen Anker, solange gerastet wird. */
    private void paintGuides(Graphics2D g2, Rectangle fit) {
        if (!guideX && !guideY) {
            return;
        }
        Point anchor = PreviewGeometry.relativeToPanel(style.getRelX(), style.getRelY(), fit);
        g2.setColor(new Color(0, 200, 255, 180));
        g2.setStroke(new BasicStroke(1f));
        if (guideX) {
            g2.drawLine(anchor.x, fit.y, anchor.x, fit.y + fit.height);
        }
        if (guideY) {
            g2.drawLine(fit.x, anchor.y, fit.x + fit.width, anchor.y);
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
