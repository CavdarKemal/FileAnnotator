package de.hasil.pictree;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import de.hasil.pictree.ui.MainFrame;

/**
 * Einstiegspunkt der PicTree-FileAnnotator-Anwendung.
 */
public final class App {

    /** Anzeigename der Anwendung. */
    public static final String APP_NAME = "PicTree FileAnnotator";

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
            System.err.println("System-Look-and-Feel nicht verfügbar: " + ex.getMessage());
        }
    }
}
