package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.Imaging;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IccProfileServiceTest {

    private final IccProfileService service = new IccProfileService();

    private File plainJpeg(Path dir, String name) throws Exception {
        File f = dir.resolve(name).toFile();
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "jpg", f);
        return f;
    }

    @Test
    void copiesIccProfileFromSourceToTarget(@TempDir Path dir) throws Exception {
        // Quelle mit eingebettetem sRGB-ICC erzeugen.
        byte[] srgb = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
        File source = plainJpeg(dir, "src.jpg");
        byte[] withIcc = IccProfileService.insertIccProfile(Files.readAllBytes(source.toPath()), srgb);
        Files.write(source.toPath(), withIcc);

        // Zielbild ohne Profil.
        File target = plainJpeg(dir, "target.jpg");

        assertTrue(service.copyIccProfile(source, target), "ICC sollte übertragen werden");

        byte[] readBack = Imaging.getIccProfileBytes(target);
        assertNotNull(readBack, "Ziel muss nun ein ICC-Profil besitzen");
        assertTrue(readBack.length > 0);
    }

    @Test
    void returnsFalseWhenSourceHasNoProfile(@TempDir Path dir) throws Exception {
        File source = plainJpeg(dir, "plain.jpg");
        File target = plainJpeg(dir, "target.jpg");
        assertFalse(service.copyIccProfile(source, target));
    }

    @Test
    void insertedJpegRemainsReadable(@TempDir Path dir) throws Exception {
        byte[] srgb = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
        File img = plainJpeg(dir, "img.jpg");
        byte[] withIcc = IccProfileService.insertIccProfile(Files.readAllBytes(img.toPath()), srgb);
        Files.write(img.toPath(), withIcc);

        BufferedImage reread = ImageIO.read(img);
        assertNotNull(reread, "JPEG mit ICC-Segment muss lesbar bleiben");
    }
}
