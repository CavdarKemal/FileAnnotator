package de.hasil.pictree;

import de.hasil.pictree.util.Logging;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

import de.hasil.pictree.service.AppSettings;
import de.hasil.pictree.ui.MainFrame;
import de.hasil.pictree.ui.Themes;

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
        AppSettings settings = new AppSettings().load();
        de.hasil.pictree.util.I18n.init(java.util.Locale.forLanguageTag(settings.getLanguage()));
        SwingUtilities.invokeLater(() -> {
            Themes.apply(settings.getTheme());
            MainFrame frame = new MainFrame(settings);
            frame.setVisible(true);
        });
    }
}
