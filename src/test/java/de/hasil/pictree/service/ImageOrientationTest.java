package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageOrientationTest {

    /** 3x2-Bild mit eindeutiger Farbe je Pixel (Position kodiert). */
    private BufferedImage distinct() {
        BufferedImage img = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                img.setRGB(x, y, (x * 40 + 1) << 8 | (y * 40 + 1));
            }
        }
        return img;
    }

    @Test
    void orientation1ReturnsSameInstance() {
        BufferedImage src = distinct();
        assertSame(src, ImageOrientation.apply(src, 1));
    }

    @Test
    void flipHorizontalOrientation2() {
        BufferedImage src = distinct();
        BufferedImage out = ImageOrientation.apply(src, 2);
        assertEquals(3, out.getWidth());
        assertEquals(2, out.getHeight());
        assertEquals(src.getRGB(2, 0), out.getRGB(0, 0));
        assertEquals(src.getRGB(0, 1), out.getRGB(2, 1));
    }

    @Test
    void rotate180Orientation3() {
        BufferedImage src = distinct();
        BufferedImage out = ImageOrientation.apply(src, 3);
        assertEquals(src.getRGB(2, 1), out.getRGB(0, 0));
        assertEquals(src.getRGB(0, 0), out.getRGB(2, 1));
    }

    @Test
    void rotate90CwOrientation6SwapsDimensions() {
        BufferedImage src = distinct(); // 3x2
        BufferedImage out = ImageOrientation.apply(src, 6);
        assertEquals(2, out.getWidth());
        assertEquals(3, out.getHeight());
        // 90° im Uhrzeigersinn: linke-untere Ecke -> linke-obere Ecke.
        assertEquals(src.getRGB(0, 1), out.getRGB(0, 0));
        // obere-linke Ecke -> obere-rechte Ecke.
        assertEquals(src.getRGB(0, 0), out.getRGB(1, 0));
    }

    @Test
    void rotate90CcwOrientation8() {
        BufferedImage src = distinct(); // 3x2
        BufferedImage out = ImageOrientation.apply(src, 8);
        assertEquals(2, out.getWidth());
        assertEquals(3, out.getHeight());
        // 90° gegen den Uhrzeigersinn: obere-rechte Ecke -> obere-linke Ecke.
        assertEquals(src.getRGB(2, 0), out.getRGB(0, 0));
    }

    @Test
    void readReturnsOneWithoutExif(@TempDir Path dir) throws Exception {
        File jpg = dir.resolve("plain.jpg").toFile();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpg", jpg);
        assertEquals(1, ImageOrientation.read(jpg));
    }

    @Test
    void readReturnsStoredOrientation(@TempDir Path dir) throws Exception {
        File jpg = dir.resolve("rot.jpg").toFile();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpg", jpg);

        TiffOutputSet set = new TiffOutputSet();
        TiffOutputDirectory root = set.getOrCreateRootDirectory();
        root.add(TiffTagConstants.TIFF_TAG_ORIENTATION, (short) 6);
        byte[] bytes = Files.readAllBytes(jpg.toPath());
        File tmp = File.createTempFile("orient-", ".jpg", dir.toFile());
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp))) {
            new ExifRewriter().updateExifMetadataLossless(bytes, os, set);
        }
        Files.move(tmp.toPath(), jpg.toPath(), StandardCopyOption.REPLACE_EXISTING);

        assertEquals(6, ImageOrientation.read(jpg));
    }
}
