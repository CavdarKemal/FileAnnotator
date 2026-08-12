package de.hasil.pictree.ui;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import de.hasil.pictree.model.StampStyle;

/**
 * Zweite Werkzeugleiste für Text-Effekte: halbtransparente Box/Banner (mit
 * Deckkraft) und Schlagschatten-Stärke.
 */
public class EffectsToolbar extends JToolBar {

    private final StampStyle style;
    private final Runnable onChange;
    private final Runnable onCommit;

    private final JToggleButton boxToggle = new JToggleButton("Box");
    private final JSlider boxOpacitySlider = new JSlider(0, 100, 45);
    private final JSlider shadowSlider = new JSlider(0, 100, 0);

    private boolean syncing;

    public EffectsToolbar(StampStyle style, Runnable onChange, Runnable onCommit) {
        this.style = style;
        this.onChange = onChange == null ? () -> { } : onChange;
        this.onCommit = onCommit == null ? () -> { } : onCommit;
        setFloatable(false);
        build();
        syncFromStyle();
    }

    private void build() {
        boxToggle.setToolTipText("Halbtransparente Box hinter dem Text");
        boxToggle.addActionListener(e -> {
            if (syncing) {
                return;
            }
            style.setBoxEnabled(boxToggle.isSelected());
            onChange.run();
            onCommit.run();
        });
        add(boxToggle);
        addSeparator();

        add(new JLabel(" Box-Deckkraft: "));
        boxOpacitySlider.setMaximumSize(new Dimension(140, 40));
        boxOpacitySlider.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            style.setBoxOpacity(boxOpacitySlider.getValue() / 100f);
            onChange.run();
            if (!boxOpacitySlider.getValueIsAdjusting()) {
                onCommit.run();
            }
        });
        add(boxOpacitySlider);
        addSeparator();

        add(new JLabel(" Schatten: "));
        shadowSlider.setMaximumSize(new Dimension(140, 40));
        shadowSlider.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            style.setShadowStrength(shadowSlider.getValue() / 100f);
            onChange.run();
            if (!shadowSlider.getValueIsAdjusting()) {
                onCommit.run();
            }
        });
        add(shadowSlider);
    }

    /** Aktualisiert die Regler aus dem aktuellen Stil. */
    public void syncFromStyle() {
        syncing = true;
        try {
            boxToggle.setSelected(style.isBoxEnabled());
            boxOpacitySlider.setValue(Math.round(style.getBoxOpacity() * 100));
            shadowSlider.setValue(Math.round(style.getShadowStrength() * 100));
        } finally {
            syncing = false;
        }
    }
}
