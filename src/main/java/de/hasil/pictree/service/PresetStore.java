package de.hasil.pictree.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hasil.pictree.model.StampStyle;
import de.hasil.pictree.util.Logging;

/**
 * Speichert benannte Stil-Vorlagen (Presets) persistent unter
 * {@code ~/.pictree/presets.properties}. Jede Vorlage ist ein
 * {@link StampStyle} unter dem Präfix {@code style.<name>.}.
 */
public class PresetStore {

    private static final Logger LOG = Logging.get(PresetStore.class);

    private final Path file;
    private final Properties props = new Properties();

    public PresetStore() {
        this(defaultPath());
    }

    public PresetStore(Path file) {
        this.file = file;
        load();
    }

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".pictree", "presets.properties");
    }

    private void load() {
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Vorlagen konnten nicht geladen werden", ex);
            }
        }
    }

    private void persist() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(w, "PicTree style presets");
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Vorlagen konnten nicht gespeichert werden", ex);
        }
    }

    /** Namen aller Vorlagen in Einfügereihenfolge. */
    public List<String> names() {
        String raw = props.getProperty("names", "");
        List<String> result = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /** Speichert (oder überschreibt) eine Vorlage. */
    public void save(String name, StampStyle style) {
        if (name == null || name.isBlank()) {
            return;
        }
        String key = name.trim();
        StampStyleCodec.write(props, prefix(key), style);
        Set<String> names = new LinkedHashSet<>(names());
        names.add(key);
        props.setProperty("names", String.join("\n", names));
        persist();
    }

    /** Lädt eine Vorlage, falls vorhanden. */
    public Optional<StampStyle> load(String name) {
        if (name == null || !names().contains(name.trim())) {
            return Optional.empty();
        }
        return Optional.of(StampStyleCodec.read(props, prefix(name.trim()), new StampStyle()));
    }

    /** Entfernt eine Vorlage. */
    public void delete(String name) {
        if (name == null) {
            return;
        }
        String key = name.trim();
        String p = prefix(key);
        props.keySet().removeIf(k -> k.toString().startsWith(p));
        Set<String> names = new LinkedHashSet<>(names());
        names.remove(key);
        props.setProperty("names", String.join("\n", names));
        persist();
    }

    private static String prefix(String name) {
        return "style." + name + ".";
    }
}
