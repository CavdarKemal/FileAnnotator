package de.hasil.pictree.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import de.hasil.pictree.model.StampStyle;
import de.hasil.pictree.service.FontRegistry;

/**
 * Werkzeugleiste zur Gestaltung des Text-Stempels: Schriftfarbe (Color-Picker),
 * Textgröße (Slider), Schriftfamilie und Fettung. Änderungen werden in ein
 * gemeinsames {@link StampStyle} geschrieben; danach wird der {@code onChange}-
 * Callback ausgelöst (typischerweise Live-Repaint der Vorschau).
 */
public class StyleToolbar extends JToolBar {

    /** Slider-Skala: Prozent der Bildhöhe (2 % .. 30 %). */
    private static final int MIN_PERCENT = 2;
    private static final int MAX_PERCENT = 30;

    private final StampStyle style;
    private final Runnable onChange;

    private final JButton colorButton = new JButton("Farbe");
    private final JComboBox<String> fontBox =
            new JComboBox<>(FontRegistry.AVAILABLE_FAMILIES.toArray(new String[0]));
    private final JToggleButton boldToggle = new JToggleButton("F");
    private final JSlider sizeSlider = new JSlider(MIN_PERCENT, MAX_PERCENT, MIN_PERCENT);

    /** Verhindert Rückkopplung, während die Widgets aus dem Style gesetzt werden. */
    private boolean syncing;

    public StyleToolbar(StampStyle style, Runnable onChange) {
        this.style = style;
        this.onChange = onChange == null ? () -> { } : onChange;
        setFloatable(false);
        buildComponents();
        syncFromStyle();
    }

    private void buildComponents() {
        // Farbe
        colorButton.setToolTipText("Schriftfarbe wählen");
        colorButton.addActionListener(e -> chooseColor());
        add(colorButton);
        addSeparator();

        // Schriftfamilie
        add(new JLabel(" Schrift: "));
        fontBox.setMaximumSize(new Dimension(150, 28));
        fontBox.addActionListener(e -> {
            if (syncing) {
                return;
            }
            style.setFontFamily((String) fontBox.getSelectedItem());
            onChange.run();
        });
        add(fontBox);
        addSeparator();

        // Fett
        boldToggle.setToolTipText("Fett");
        boldToggle.setFont(boldToggle.getFont().deriveFont(Font.BOLD));
        boldToggle.addActionListener(e -> {
            if (syncing) {
                return;
            }
            style.setFontStyle(boldToggle.isSelected() ? Font.BOLD : Font.PLAIN);
            onChange.run();
        });
        add(boldToggle);
        addSeparator();

        // Größe
        add(new JLabel(" Größe: "));
        sizeSlider.setMajorTickSpacing(7);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setMaximumSize(new Dimension(180, 40));
        sizeSlider.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            style.setRelativeSize(sizeSlider.getValue() / 100f);
            onChange.run();
        });
        add(sizeSlider);
    }

    /** Aktualisiert alle Bedienelemente anhand des aktuellen {@link StampStyle}. */
    public void syncFromStyle() {
        syncing = true;
        try {
            updateColorSwatch();
            fontBox.setSelectedItem(style.getFontFamily());
            boldToggle.setSelected((style.getFontStyle() & Font.BOLD) != 0);
            int percent = Math.round(style.getRelativeSize() * 100f);
            sizeSlider.setValue(Math.min(MAX_PERCENT, Math.max(MIN_PERCENT, percent)));
        } finally {
            syncing = false;
        }
    }

    private void chooseColor() {
        Color chosen = JColorChooser.showDialog(this, "Schriftfarbe", style.getColor());
        if (chosen != null) {
            style.setColor(chosen);
            updateColorSwatch();
            onChange.run();
        }
    }

    private void updateColorSwatch() {
        colorButton.setIcon(new ColorSwatchIcon(style.getColor(), 16, 16));
    }

    /** Kleines Farbfeld als Button-Icon. */
    private static final class ColorSwatchIcon implements javax.swing.Icon {
        private final Color color;
        private final int w;
        private final int h;

        ColorSwatchIcon(Color color, int w, int h) {
            this.color = color;
            this.w = w;
            this.h = h;
        }

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, w, h);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, w, h);
        }

        @Override
        public int getIconWidth() {
            return w;
        }

        @Override
        public int getIconHeight() {
            return h;
        }
    }
}
