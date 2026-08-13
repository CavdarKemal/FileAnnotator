package de.hasil.pictree.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hasil.pictree.model.ImageTypeFilter;
import de.hasil.pictree.model.LogoOverlay;
import de.hasil.pictree.model.StampStyle;
import de.hasil.pictree.util.Logging;

/**
 * Stapelverarbeitung: wendet denselben Text-Stempel (mit relativer Position und
 * Größe → konsistent über unterschiedliche Bildgrößen) auf alle Bilder eines
 * Ordners an, speichert jedes Ergebnis über den {@link SaveService} und
 * überträgt die EXIF-Daten je Bild.
 */
public class BatchService {

    /** Ein fehlgeschlagenes Bild mit Begründung. */
    public record Failure(File file, String reason) {
    }

    /** Standard-Unterordner, falls kein (gültiger) Stempeltext für den Namen vorliegt. */
    public static final String OUTPUT_SUBDIR = "_stamped";

    /** Maximallänge des aus dem Stempeltext abgeleiteten Ordnernamens. */
    private static final int MAX_FOLDER_NAME = 60;

    /** Unter Windows reservierte Gerätenamen (case-insensitiv). */
    private static final java.util.Set<String> RESERVED_NAMES = java.util.Set.of("con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    /**
     * Leitet aus dem Stempeltext einen dateisystem-sicheren Ordnernamen ab:
     * unzulässige Zeichen werden entfernt, Zeilenumbrüche zu Leerzeichen, die
     * Länge begrenzt. Ist das Ergebnis leer oder reserviert, wird ein sicherer
     * Ersatz ({@link #OUTPUT_SUBDIR}) verwendet.
     */
    public static String sanitizeFolderName(String text) {
        if (text == null) {
            return OUTPUT_SUBDIR;
        }
        String s = text.replaceAll("[\\r\\n\\t]+", " ");
        s = s.replaceAll("[\\\\/:*?\"<>|]", "");   // unter Windows/Unix unzulässige Zeichen
        s = s.replaceAll("[\\x00-\\x1f]", "");     // Steuerzeichen
        s = s.replaceAll("\\s+", " ").trim();
        s = s.replaceAll("^[.\\s]+", "").replaceAll("[.\\s]+$", ""); // führende/abschließende . und Space
        if (s.length() > MAX_FOLDER_NAME) {
            s = s.substring(0, MAX_FOLDER_NAME).trim();
        }
        if (s.isEmpty()) {
            return OUTPUT_SUBDIR;
        }
        if (RESERVED_NAMES.contains(s.toLowerCase(java.util.Locale.ROOT))) {
            return s + "_";
        }
        return s;
    }

    /** Ergebnis eines Stapellaufs inkl. tatsächlichem Zielordner. */
    public record BatchResult(List<File> saved, List<Failure> failed, File outputDir) {
        public int total() {
            return saved.size() + failed.size();
        }
    }

    /** Fortschritts-Rückmeldung (1-basiert: {@code done} von {@code total}). */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int done, int total, File current, File savedOrNull);
    }

    private static final Logger LOG = Logging.get(BatchService.class);

    private final SaveService saveService;
    private final ExifService exifService;
    private final IccProfileService iccService = new IccProfileService();

    public BatchService(SaveService saveService, ExifService exifService) {
        this.saveService = saveService;
        this.exifService = exifService;
    }

    /** Listet die (nicht-rekursiven) Bilddateien eines Ordners, alphabetisch sortiert. */
    public static List<File> listImages(File folder) {
        return listImages(folder, null);
    }

    /**
     * Wie {@link #listImages(File)}, beschränkt aber auf die vom {@code filter}
     * zugelassenen Bildtypen. {@code filter == null} bedeutet: alle unterstützten
     * Bildformate.
     */
    public static List<File> listImages(File folder, ImageTypeFilter filter) {
        if (folder == null || !folder.isDirectory()) {
            return List.of();
        }
        java.io.FileFilter accept = filter == null ? ImageSupport::isImageFile : filter::acceptsImage;
        File[] files = folder.listFiles(accept);
        if (files == null) {
            return List.of();
        }
        List<File> list = new ArrayList<>(Arrays.asList(files));
        list.sort(Comparator.comparing(f -> f.getName().toLowerCase()));
        return list;
    }

    /**
     * Verarbeitet alle Bilder in {@code folder}. Die Ergebnisse landen im
     * Unterordner {@link #OUTPUT_SUBDIR} des Quellordners; die Originale bleiben
     * unberührt.
     *
     * @param folder Quellordner
     * @param text   Stempeltext
     * @param style  Stil/relative Position (für alle Bilder identisch)
     * @param cb     optionaler Fortschritts-Callback (darf {@code null} sein)
     */
    public BatchResult processFolder(File folder, String text, StampStyle style, ProgressCallback cb) {
        return processFolder(folder, text, style, null, cb);
    }

    /** Wie oben, zusätzlich mit optionalem Logo-Overlay. */
    public BatchResult processFolder(File folder, String text, StampStyle style, LogoOverlay logo,
            ProgressCallback cb) {
        File outputDir = new File(folder, OUTPUT_SUBDIR);
        return processFiles(listImages(folder), outputDir, text, style, logo, cb);
    }

    /**
     * Verarbeitet eine explizite Liste von Bildern und schreibt die Ergebnisse
     * nach {@code outputDir}. Grundlage für die Stapelverarbeitung einer im
     * Thumbnail-Streifen getroffenen Auswahl.
     *
     * @param images    zu stempelnde Bilddateien
     * @param outputDir Zielordner (wird bei Bedarf angelegt)
     * @param text      Stempeltext
     * @param style     Stil/relative Position (für alle Bilder identisch)
     * @param logo      optionales Logo-Overlay (darf {@code null} sein)
     * @param cb        optionaler Fortschritts-Callback (darf {@code null} sein)
     */
    public BatchResult processFiles(List<File> images, File outputDir, String text, StampStyle style,
            LogoOverlay logo, ProgressCallback cb) {
        // Eigener SaveService pro Lauf: schreibt in den gewählten Zielordner,
        // nicht in den Album-Ordner des geteilten saveService.
        SaveService out = new SaveService(outputDir.toPath(), saveService.getQuality());
        List<File> saved = new ArrayList<>();
        List<Failure> failed = new ArrayList<>();

        int total = images.size();
        int done = 0;
        for (File image : images) {
            done++;
            File outFile = null;
            try {
                BufferedImage src = ImageSupport.load(image);
                if (src == null) {
                    failed.add(new Failure(image, "Bild konnte nicht geladen werden"));
                } else {
                    // Platzhalter je Bild auflösen (z. B. {datum}, {dateiname}).
                    String resolved = PlaceholderResolver.resolve(text, image);
                    BufferedImage stamped = ImageStampService.renderStamp(src, resolved, style, logo);
                    outFile = out.save(stamped, image.getName());
                    exifService.copyExif(image, outFile);
                    iccService.copyIccProfile(image, outFile);
                    saved.add(outFile);
                }
            } catch (Exception ex) {
                String reason = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                failed.add(new Failure(image, reason));
                LOG.log(Level.WARNING, "Batch: Fehler bei " + image, ex);
            }
            if (cb != null) {
                cb.onProgress(done, total, image, outFile);
            }
        }
        return new BatchResult(saved, failed, outputDir);
    }
}
