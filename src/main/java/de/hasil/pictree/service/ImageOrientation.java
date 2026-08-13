package de.hasil.pictree.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

import de.hasil.pictree.util.Logging;

/**
 * Liest die EXIF-Orientierung (1..8) und wendet sie pixelbasiert auf ein Bild an.
 * {@code ImageIO} ignoriert die Orientierung – ohne Korrektur würden hochkant
 * aufgenommene Fotos gedreht dargestellt und gespeichert.
 */
public final class ImageOrientation {

    private static final Logger LOG = Logging.get(ImageOrientation.class);

    private ImageOrientation() {
    }

    /** Liest den EXIF-Orientation-Wert (1 = normal) oder 1, falls nicht vorhanden. */
    public static int read(File file) {
        try {
            ImageMetadata metadata = Imaging.getMetadata(file);
            if (metadata instanceof JpegImageMetadata jpeg) {
                TiffField field = jpeg.findExifValueWithExactMatch(TiffTagConstants.TIFF_TAG_ORIENTATION);
                if (field != null) {
                    int value = field.getIntValue();
                    if (value >= 1 && value <= 8) {
                        return value;
                    }
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Orientierung nicht lesbar für " + file + ": " + ex.getMessage());
        }
        return 1;
    }

    /**
     * Gibt ein neu ausgerichtetes Bild zurück. Bei Orientierung 1 (oder ungültig)
     * wird das Originalbild unverändert zurückgeliefert.
     */
    public static BufferedImage apply(BufferedImage src, int orientation) {
        if (src == null || orientation <= 1 || orientation > 8) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        boolean swapDims = orientation >= 5;
        int dw = swapDims ? h : w;
        int dh = swapDims ? w : h;

        BufferedImage dst = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                int sx;
                int sy;
                switch (orientation) {
                    case 2 -> { sx = w - 1 - x; sy = y; }             // horizontal spiegeln
                    case 3 -> { sx = w - 1 - x; sy = h - 1 - y; }     // 180°
                    case 4 -> { sx = x; sy = h - 1 - y; }             // vertikal spiegeln
                    case 5 -> { sx = y; sy = x; }                     // transponieren
                    case 6 -> { sx = y; sy = h - 1 - x; }             // 90° im Uhrzeigersinn
                    case 7 -> { sx = w - 1 - y; sy = h - 1 - x; }     // transversal
                    case 8 -> { sx = w - 1 - y; sy = x; }             // 90° gegen den Uhrzeigersinn
                    default -> { sx = x; sy = y; }
                }
                dst.setRGB(x, y, src.getRGB(sx, sy));
            }
        }
        return dst;
    }

    /** Liest die Orientierung und wendet sie direkt an. */
    public static BufferedImage correct(File file, BufferedImage src) {
        return apply(src, read(file));
    }
}
