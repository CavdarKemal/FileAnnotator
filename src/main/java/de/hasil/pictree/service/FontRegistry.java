package de.hasil.pictree.service;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hasil.pictree.util.Logging;

/**
 * Zentrale Font-Auflösung. Registriert beim Laden die gebündelte Roboto-Schrift
 * (Resource {@code /fonts/Roboto-Regular.ttf}) und stellt sie neben den
 * JDK-Standardfamilien zur Verfügung. Fehlt die Resource, wird "Roboto"
 * transparent auf die Standard-Sans-Serif abgebildet.
 */
public final class FontRegistry {

    /** Familienname der gebündelten Schrift. */
    public static final String ROBOTO = "Roboto";

    private static final Logger LOG = Logging.get(FontRegistry.class);
    private static final Font ROBOTO_BASE = loadRoboto();

    /**
     * Auswählbare Familien für die Werkzeugleiste: zuerst die gebündelte Roboto
     * und die logischen JDK-Familien, danach alle im System installierten
     * Schriftfamilien (alphabetisch, dedupliziert).
     */
    public static final List<String> AVAILABLE_FAMILIES = buildAvailableFamilies();

    private FontRegistry() {
    }

    private static List<String> buildAvailableFamilies() {
        Set<String> families = new LinkedHashSet<>();
        families.add(ROBOTO);
        families.add("SansSerif");
        families.add("Serif");
        families.add("Monospaced");
        try {
            String[] system = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Arrays.sort(system, String.CASE_INSENSITIVE_ORDER);
            for (String family : system) {
                if (family != null && !family.isBlank()) {
                    families.add(family);
                }
            }
        } catch (RuntimeException ex) {
            LOG.log(Level.FINE, "System-Schriften nicht ermittelbar: {0}", ex.getMessage());
        }
        return List.copyOf(families);
    }

    private static Font loadRoboto() {
        try (InputStream in = FontRegistry.class.getResourceAsStream("/fonts/Roboto-Regular.ttf")) {
            if (in == null) {
                return null;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            // Registrierung ermöglicht auch die Nutzung über den Familiennamen.
            try {
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            } catch (RuntimeException ignored) {
                // Registrierung ist optional; deriveFont funktioniert auch ohne.
            }
            return font;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Roboto konnte nicht geladen werden: {0}", ex.getMessage());
            return null;
        }
    }

    /** True, wenn die gebündelte Roboto-Schrift verfügbar ist. */
    public static boolean isRobotoAvailable() {
        return ROBOTO_BASE != null;
    }

    /** Liefert einen Font für die gegebene Familie/Stil/Größe. */
    public static Font resolve(String family, int style, int size) {
        int safeSize = Math.max(1, size);
        if (ROBOTO.equalsIgnoreCase(family) && ROBOTO_BASE != null) {
            return ROBOTO_BASE.deriveFont(style, (float) safeSize);
        }
        return new Font(family, style, safeSize);
    }
}
