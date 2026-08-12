package de.hasil.pictree.service;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Properties;

import de.hasil.pictree.model.Annotation;
import de.hasil.pictree.model.StampStyle;

/**
 * Persistiert Kommentar, Stil und Position pro Bild in einer Sidecar-Datei
 * ({@code <bildname>.pictree.properties}) neben dem Original. Beim erneuten
 * Selektieren wird die Annotation wiederhergestellt. Kein zusätzliches
 * Abhängigkeitspaket nötig – reine {@link Properties}.
 */
public class AnnotationStore {

    /** Endung der Sidecar-Datei. */
    public static final String SUFFIX = ".pictree.properties";

    /** Sidecar-Datei zu einem Bild (Endung wird angehängt, Originalname bleibt erhalten). */
    public File sidecarFor(File image) {
        return new File(image.getParentFile(), image.getName() + SUFFIX);
    }

    public boolean exists(File image) {
        return sidecarFor(image).isFile();
    }

    /** Schreibt die Annotation als Properties-Datei. */
    public void save(File image, String comment, StampStyle style) throws IOException {
        Properties p = new Properties();
        p.setProperty("comment", comment == null ? "" : comment);
        StampStyleCodec.write(p, "", style);

        File sidecar = sidecarFor(image);
        try (Writer w = Files.newBufferedWriter(sidecar.toPath(), StandardCharsets.UTF_8)) {
            p.store(w, "PicTree annotation for " + image.getName());
        }
    }

    /** Lädt die Annotation, falls vorhanden und lesbar. */
    public Optional<Annotation> load(File image) {
        File sidecar = sidecarFor(image);
        if (!sidecar.isFile()) {
            return Optional.empty();
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(sidecar.toPath(), StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException ex) {
            return Optional.empty();
        }

        StampStyle style = StampStyleCodec.read(p, "", new StampStyle());
        String comment = p.getProperty("comment", "");
        return Optional.of(new Annotation(comment, style));
    }

    /** Entfernt die Sidecar-Datei. Gibt {@code true} zurück, wenn eine gelöscht wurde. */
    public boolean delete(File image) {
        File sidecar = sidecarFor(image);
        return sidecar.exists() && sidecar.delete();
    }
}
