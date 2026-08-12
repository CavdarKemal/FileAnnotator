package de.hasil.pictree.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Speichert bestempelte Bilder als neue JPG-Dateien im Zielordner
 * (standardmäßig {@code ~/Pictures/PicTreeAlbums}). Dateinamen werden
 * kollisionsfrei vergeben; das Original wird nie berührt.
 */
public class SaveService {

    /** Unterordner im Benutzerprofil. */
    public static final String ALBUM_SUBDIR = "Pictures/PicTreeAlbums";

    /** Standard-JPEG-Qualität, falls nicht anders angegeben. */
    public static final float DEFAULT_QUALITY = 0.92f;

    private final Path targetDir;
    private final float quality;

    /** Verwendet den Standard-Album-Ordner im Benutzerprofil und Standardqualität. */
    public SaveService() {
        this(defaultAlbumDir(), DEFAULT_QUALITY);
    }

    /** Expliziter Zielordner, Standardqualität (z. B. für Tests). */
    public SaveService(Path targetDir) {
        this(targetDir, DEFAULT_QUALITY);
    }

    /** Expliziter Zielordner und JPEG-Qualität (0.1..1.0). */
    public SaveService(Path targetDir, float quality) {
        this.targetDir = targetDir;
        this.quality = clampQuality(quality);
    }

    public float getQuality() {
        return quality;
    }

    private static float clampQuality(float q) {
        if (q < 0.1f) {
            return 0.1f;
        }
        return Math.min(q, 1.0f);
    }

    /** Standard-Zielordner: {@code <user.home>/Pictures/PicTreeAlbums}. */
    public static Path defaultAlbumDir() {
        return Path.of(System.getProperty("user.home"), ALBUM_SUBDIR);
    }

    public Path getTargetDir() {
        return targetDir;
    }

    /**
     * Schreibt {@code image} als JPG. Der Dateiname basiert auf
     * {@code originalName} (Endung wird durch .jpg ersetzt) und wird bei
     * Kollision um ein Suffix erweitert.
     *
     * @return die tatsächlich geschriebene Datei
     */
    public File save(BufferedImage image, String originalName) throws IOException {
        Files.createDirectories(targetDir);
        File target = resolveCollisionFreeTarget(originalName);
        writeJpeg(image, target);
        return target;
    }

    private File resolveCollisionFreeTarget(String originalName) {
        String base = stripExtension(originalName);
        if (base.isBlank()) {
            base = "bild";
        }
        File candidate = targetDir.resolve(base + ".jpg").toFile();
        int counter = 1;
        while (candidate.exists()) {
            candidate = targetDir.resolve(base + "-" + counter + ".jpg").toFile();
            counter++;
        }
        return candidate;
    }

    private void writeJpeg(BufferedImage image, File target) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("Kein JPEG-Writer verfügbar");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(target)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private static String stripExtension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }
}
