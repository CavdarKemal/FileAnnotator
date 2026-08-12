package de.hasil.pictree;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import de.hasil.pictree.ui.MainFrame;
import de.hasil.pictree.util.Logging;

/**
 * Einstiegspunkt der PicTree-FileAnnotator-Anwendung.
 */
public final class App {

    /** Anzeigename der Anwendung. */
    public static final String APP_NAME = "PicTree FileAnnotator";

    private static final Logger LOG = Logging.get(App.class);

    private App() {
        // Utility-/Launcher-Klasse
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /** Setzt das System-Look-and-Feel (Windows-Explorer-Optik). */
    public static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // Fallback auf das Standard-L&F ist unkritisch.
            LOG.log(Level.WARNING, "System-Look-and-Feel nicht verfügbar: {0}", ex.getMessage());
        }
    }
}
