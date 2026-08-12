package de.hasil.pictree.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import de.hasil.pictree.service.AppSettings;
import de.hasil.pictree.util.Logging;

/**
 * Installiert das FlatLaf-Look-and-Feel (hell/dunkel). Fällt bei Fehlern auf das
 * System-Look-and-Feel zurück.
 */
public final class Themes {

    private static final Logger LOG = Logging.get(Themes.class);

    private Themes() {
    }

    /**
     * Aktiviert das gewünschte Theme.
     *
     * @param theme {@link AppSettings#THEME_DARK} oder sonst hell
     * @return {@code true} bei Erfolg
     */
    public static boolean apply(String theme) {
        try {
            if (AppSettings.THEME_DARK.equals(theme)) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            return true;
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Theme konnte nicht gesetzt werden: " + theme, ex);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception fallback) {
                LOG.log(Level.WARNING, "System-L&F-Fallback fehlgeschlagen", fallback);
            }
            return false;
        }
    }

    /** True, wenn aktuell ein FlatLaf aktiv ist. */
    public static boolean isFlatLafActive() {
        return UIManager.getLookAndFeel() instanceof com.formdev.flatlaf.FlatLaf;
    }
}
