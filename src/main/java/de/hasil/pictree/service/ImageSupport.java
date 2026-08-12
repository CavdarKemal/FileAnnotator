package de.hasil.pictree.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

/**
 * Hilfsfunktionen zum Erkennen und Laden von Bilddateien.
 */
public final class ImageSupport {

    /** Unterstützte Bild-Endungen (Lesen). */
    public static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "bmp", "wbmp", "tif", "tiff");

    private ImageSupport() {
    }

    /** Prüft anhand der Dateiendung, ob es sich um eine unterstützte Bilddatei handelt. */
    public static boolean isImageFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        return IMAGE_EXTENSIONS.contains(extensionOf(file.getName()));
    }

    /** Liefert die Dateiendung in Kleinbuchstaben ohne Punkt (leer, falls keine). */
    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Lädt ein Bild und korrigiert dabei die EXIF-Orientierung, oder gibt
     * {@code null} zurück, wenn es nicht gelesen werden kann.
     */
    public static BufferedImage load(File file) {
        if (!isImageFile(file)) {
            return null;
        }
        try {
            BufferedImage raw = ImageIO.read(file);
            if (raw == null) {
                return null;
            }
            return ImageOrientation.correct(file, raw);
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }
}
