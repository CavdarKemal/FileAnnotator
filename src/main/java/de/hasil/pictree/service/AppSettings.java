package de.hasil.pictree.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hasil.pictree.model.StampStyle;
import de.hasil.pictree.util.Logging;

/**
 * Persistente Anwendungseinstellungen (Fenstergröße, zuletzt genutzter Ordner,
 * Zielordner, JPEG-Qualität, Standard-Stil, Theme). Wird als Properties-Datei
 * unter {@code ~/.pictree/settings.properties} gespeichert.
 */
public class AppSettings {

    private static final Logger LOG = Logging.get(AppSettings.class);

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private final Path file;
    private final Properties props = new Properties();

    public AppSettings() {
        this(defaultPath());
    }

    public AppSettings(Path file) {
        this.file = file;
    }

    /** Standardpfad: {@code <user.home>/.pictree/settings.properties}. */
    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".pictree", "settings.properties");
    }

    /** Lädt die Einstellungen von der Platte (falls vorhanden). */
    public AppSettings load() {
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Einstellungen konnten nicht geladen werden", ex);
            }
        }
        return this;
    }

    /** Schreibt die Einstellungen auf die Platte. */
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(w, "PicTree settings");
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Einstellungen konnten nicht gespeichert werden", ex);
        }
    }

    // --- Fenster -------------------------------------------------------------

    public int getWindowWidth() {
        return StampStyleCodec.parseInt(props, "window.width", 1000);
    }

    public int getWindowHeight() {
        return StampStyleCodec.parseInt(props, "window.height", 680);
    }

    public void setWindowSize(int width, int height) {
        props.setProperty("window.width", Integer.toString(width));
        props.setProperty("window.height", Integer.toString(height));
    }

    // --- Ordner --------------------------------------------------------------

    public String getLastFolder() {
        return props.getProperty("lastFolder", "");
    }

    public void setLastFolder(String path) {
        props.setProperty("lastFolder", path == null ? "" : path);
    }

    /** Maximale Anzahl gemerkter Ordner. */
    public static final int MAX_RECENT = 8;

    /** Zuletzt verwendete Ordner, neueste zuerst. */
    public List<String> getRecentFolders() {
        String raw = props.getProperty("recentFolders", "");
        List<String> result = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /** Fügt einen Ordner vorne ein (dedupliziert, gekappt auf {@link #MAX_RECENT}). */
    public void addRecentFolder(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        LinkedList<String> list = new LinkedList<>(getRecentFolders());
        list.remove(path);
        list.addFirst(path);
        while (list.size() > MAX_RECENT) {
            list.removeLast();
        }
        props.setProperty("recentFolders", String.join("\n", list));
    }

    public String getTargetDir() {
        return props.getProperty("targetDir", SaveService.defaultAlbumDir().toString());
    }

    public void setTargetDir(String path) {
        props.setProperty("targetDir", path == null ? "" : path);
    }

    // --- Speichern -----------------------------------------------------------

    public float getJpegQuality() {
        return (float) StampStyleCodec.parseDouble(props, "jpegQuality", 0.92);
    }

    public void setJpegQuality(float quality) {
        props.setProperty("jpegQuality", Float.toString(clamp(quality)));
    }

    // --- Theme ---------------------------------------------------------------

    public String getLanguage() {
        return props.getProperty("language", "de");
    }

    public void setLanguage(String lang) {
        props.setProperty("language", "en".equals(lang) ? "en" : "de");
    }

    public String getTheme() {
        return props.getProperty("theme", THEME_LIGHT);
    }

    public void setTheme(String theme) {
        props.setProperty("theme", THEME_DARK.equals(theme) ? THEME_DARK : THEME_LIGHT);
    }

    // --- Standard-Stil -------------------------------------------------------

    public StampStyle getDefaultStyle() {
        return StampStyleCodec.read(props, "defaultStyle.", new StampStyle());
    }

    public void setDefaultStyle(StampStyle style) {
        StampStyleCodec.write(props, "defaultStyle.", style);
    }

    private static float clamp(float q) {
        if (q < 0.1f) {
            return 0.1f;
        }
        return Math.min(q, 1.0f);
    }
}
