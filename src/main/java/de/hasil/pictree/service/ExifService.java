package de.hasil.pictree.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

/**
 * Überträgt die EXIF-Metadaten (u. a. Aufnahmedatum und GPS-Ort) vom
 * Originalbild in eine neu erzeugte JPG-Datei. So wird das gestempelte Bild in
 * Galerien zeitlich und geografisch korrekt einsortiert.
 *
 * <p>Robust ausgelegt: Kann das Original keine EXIF liefern (z. B. PNG oder
 * JPEG ohne Metadaten), passiert nichts und es wird {@code false} gemeldet.
 */
public class ExifService {

    /**
     * Kopiert die EXIF-Daten von {@code source} nach {@code target} (JPEG,
     * verlustfrei bzgl. Bilddaten).
     *
     * @return {@code true}, wenn EXIF übertragen wurde; {@code false}, wenn die
     *         Quelle keine EXIF besitzt oder ein Fehler auftrat.
     */
    public boolean copyExif(File source, File target) {
        try {
            TiffOutputSet outputSet = readExifOutputSet(source);
            if (outputSet == null) {
                return false;
            }
            byte[] targetBytes = Files.readAllBytes(target.toPath());
            File tmp = File.createTempFile("pictree-exif-", ".jpg", target.getParentFile());
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp))) {
                new ExifRewriter().updateExifMetadataLossless(targetBytes, os, outputSet);
            }
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception ex) {
            System.err.println("EXIF-Kopie fehlgeschlagen (" + source + " -> " + target + "): "
                    + ex.getMessage());
            return false;
        }
    }

    /** Liest die EXIF des Originals als {@link TiffOutputSet} oder {@code null}. */
    private TiffOutputSet readExifOutputSet(File source) throws Exception {
        ImageMetadata metadata = Imaging.getMetadata(source);
        if (metadata instanceof JpegImageMetadata jpeg) {
            TiffImageMetadata exif = jpeg.getExif();
            if (exif != null) {
                return exif.getOutputSet();
            }
        }
        return null;
    }
}
