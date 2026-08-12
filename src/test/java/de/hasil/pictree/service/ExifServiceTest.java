package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExifServiceTest {

    private final ExifService service = new ExifService();

    /** Schreibt ein einfaches JPEG. */
    private File writeJpeg(Path dir, String name, int w, int h) throws Exception {
        File f = dir.resolve(name).toFile();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "jpg", f);
        return f;
    }

    /** Fügt einem vorhandenen JPEG DateTimeOriginal + GPS hinzu. */
    private void addExif(File jpeg, String dateTimeOriginal, double lon, double lat) throws Exception {
        TiffOutputSet outputSet = new TiffOutputSet();
        TiffOutputDirectory exif = outputSet.getOrCreateExifDirectory();
        exif.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        exif.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, dateTimeOriginal);
        outputSet.setGpsInDegrees(lon, lat);

        byte[] bytes = Files.readAllBytes(jpeg.toPath());
        File tmp = File.createTempFile("src-exif-", ".jpg", jpeg.getParentFile());
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp))) {
            new ExifRewriter().updateExifMetadataLossless(bytes, os, outputSet);
        }
        Files.move(tmp.toPath(), jpeg.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void copiesDateAndGpsFromOriginalToTarget(@TempDir Path dir) throws Exception {
        File original = writeJpeg(dir, "orig.jpg", 32, 24);
        addExif(original, "2020:01:02 03:04:05", 11.0, 48.0);
        File target = writeJpeg(dir, "target.jpg", 32, 24);

        assertTrue(service.copyExif(original, target), "EXIF sollte übertragen werden");

        JpegImageMetadata meta = (JpegImageMetadata) Imaging.getMetadata(target);
        assertNotNull(meta);

        TiffField dto = meta.findExifValueWithExactMatch(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        assertNotNull(dto, "DateTimeOriginal muss vorhanden sein");
        assertTrue(dto.getStringValue().startsWith("2020:01:02 03:04:05"));

        TiffImageMetadata exif = meta.getExif();
        assertNotNull(exif);
        TiffImageMetadata.GpsInfo gps = exif.getGpsInfo();
        assertNotNull(gps, "GPS-Info muss vorhanden sein");
        assertTrue(Math.abs(gps.getLongitudeAsDegreesEast() - 11.0) < 0.001);
        assertTrue(Math.abs(gps.getLatitudeAsDegreesNorth() - 48.0) < 0.001);
    }

    @Test
    void returnsFalseWhenOriginalHasNoExif(@TempDir Path dir) throws Exception {
        File original = writeJpeg(dir, "plain.jpg", 16, 16);
        File target = writeJpeg(dir, "target.jpg", 16, 16);
        assertFalse(service.copyExif(original, target));
    }

    @Test
    void returnsFalseForNonJpegSource(@TempDir Path dir) throws Exception {
        File png = dir.resolve("orig.png").toFile();
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", png);
        File target = writeJpeg(dir, "target.jpg", 16, 16);
        assertFalse(service.copyExif(png, target));
    }
}
