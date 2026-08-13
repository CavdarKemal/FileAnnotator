package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlaceholderResolverTest {

    private File jpegWithExif(Path dir, String name) throws Exception {
        File f = dir.resolve(name).toFile();
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "jpg", f);

        TiffOutputSet set = new TiffOutputSet();
        TiffOutputDirectory exif = set.getOrCreateExifDirectory();
        exif.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, "2021:07:15 12:00:00");
        TiffOutputDirectory root = set.getOrCreateRootDirectory();
        root.add(TiffTagConstants.TIFF_TAG_MODEL, "TestCam");
        set.setGpsInDegrees(11.0, 48.0);

        byte[] bytes = Files.readAllBytes(f.toPath());
        File tmp = File.createTempFile("ph-", ".jpg", dir.toFile());
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp))) {
            new ExifRewriter().updateExifMetadataLossless(bytes, os, set);
        }
        Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return f;
    }

    @Test
    void resolvesFilenameDateGpsAndModel(@TempDir Path dir) throws Exception {
        File img = jpegWithExif(dir, "urlaub.jpg");

        assertEquals("urlaub", PlaceholderResolver.resolve("{dateiname}", img));
        assertEquals("2021-07-15", PlaceholderResolver.resolve("{datum}", img));
        assertEquals("TestCam", PlaceholderResolver.resolve("{exif:model}", img));

        String gps = PlaceholderResolver.resolve("{gps}", img);
        assertTrue(gps.startsWith("48."), gps);
        assertTrue(gps.contains("11."), gps);
    }

    @Test
    void combinesMultiplePlaceholders(@TempDir Path dir) throws Exception {
        File img = jpegWithExif(dir, "foto.jpg");
        assertEquals("foto – 2021-07-15", PlaceholderResolver.resolve("{dateiname} – {datum}", img));
    }

    @Test
    void plainTextReturnedUnchanged(@TempDir Path dir) throws Exception {
        File img = jpegWithExif(dir, "x.jpg");
        assertEquals("Nur Text", PlaceholderResolver.resolve("Nur Text", img));
    }

    @Test
    void fallsBackToFileDateWithoutExif(@TempDir Path dir) throws Exception {
        File plain = dir.resolve("plain.jpg").toFile();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpg", plain);

        String datum = PlaceholderResolver.resolve("{datum}", plain);
        assertTrue(datum.matches("\\d{4}-\\d{2}-\\d{2}"), datum);
        assertEquals("", PlaceholderResolver.resolve("{gps}", plain));
        assertEquals("", PlaceholderResolver.resolve("{exif:model}", plain));
    }
}
