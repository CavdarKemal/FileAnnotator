package de.hasil.pictree.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /** Ergebnis eines Stapellaufs. */
    public record BatchResult(List<File> saved, List<File> failed) {
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
        if (folder == null || !folder.isDirectory()) {
            return List.of();
        }
        File[] files = folder.listFiles(ImageSupport::isImageFile);
        if (files == null) {
            return List.of();
        }
        List<File> list = new ArrayList<>(Arrays.asList(files));
        list.sort(Comparator.comparing(f -> f.getName().toLowerCase()));
        return list;
    }

    /**
     * Verarbeitet alle Bilder in {@code folder}.
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
        List<File> images = listImages(folder);
        List<File> saved = new ArrayList<>();
        List<File> failed = new ArrayList<>();

        int total = images.size();
        int done = 0;
        for (File image : images) {
            done++;
            File outFile = null;
            try {
                BufferedImage src = ImageSupport.load(image);
                if (src == null) {
                    failed.add(image);
                } else {
                    // Platzhalter je Bild auflösen (z. B. {datum}, {dateiname}).
                    String resolved = PlaceholderResolver.resolve(text, image);
                    BufferedImage stamped = ImageStampService.renderStamp(src, resolved, style, logo);
                    outFile = saveService.save(stamped, image.getName());
                    exifService.copyExif(image, outFile);
                    iccService.copyIccProfile(image, outFile);
                    saved.add(outFile);
                }
            } catch (Exception ex) {
                failed.add(image);
                LOG.log(Level.WARNING, "Batch: Fehler bei " + image, ex);
            }
            if (cb != null) {
                cb.onProgress(done, total, image, outFile);
            }
        }
        return new BatchResult(saved, failed);
    }
}
