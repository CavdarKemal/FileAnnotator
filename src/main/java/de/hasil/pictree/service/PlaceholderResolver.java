package de.hasil.pictree.service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;

import de.hasil.pictree.util.Logging;

/**
 * Ersetzt Platzhalter im Stempeltext durch bild-spezifische Werte:
 * <ul>
 *   <li>{@code {dateiname}} – Dateiname ohne Endung</li>
 *   <li>{@code {datum}} – Aufnahmedatum (EXIF) oder Dateidatum, {@code yyyy-MM-dd}</li>
 *   <li>{@code {gps}} – "Breite, Länge" aus EXIF-GPS</li>
 *   <li>{@code {exif:make}}, {@code {exif:model}} – Kamerahersteller/-modell</li>
 * </ul>
 * Unbekannte oder nicht vorhandene Werte werden durch einen leeren String ersetzt.
 */
public final class PlaceholderResolver {

    private static final Logger LOG = Logging.get(PlaceholderResolver.class);

    private PlaceholderResolver() {
    }

    /** Ersetzt alle Platzhalter in {@code template} bezogen auf {@code image}. */
    public static String resolve(String template, File image) {
        if (template == null || template.isEmpty() || template.indexOf('{') < 0) {
            return template == null ? "" : template;
        }
        JpegImageMetadata meta = readJpegMetadata(image);

        String result = template;
        result = result.replace("{dateiname}", baseName(image));
        result = result.replace("{datum}", date(meta, image));
        result = result.replace("{gps}", gps(meta));
        result = result.replace("{exif:make}", exifString(meta, TiffTagConstants.TIFF_TAG_MAKE));
        result = result.replace("{exif:model}", exifString(meta, TiffTagConstants.TIFF_TAG_MODEL));
        return result;
    }

    private static JpegImageMetadata readJpegMetadata(File image) {
        if (image == null) {
            return null;
        }
        try {
            ImageMetadata metadata = Imaging.getMetadata(image);
            if (metadata instanceof JpegImageMetadata jpeg) {
                return jpeg;
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Keine EXIF-Metadaten für " + image + ": " + ex.getMessage());
        }
        return null;
    }

    private static String baseName(File image) {
        if (image == null) {
            return "";
        }
        String name = image.getName();
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    private static String date(JpegImageMetadata meta, File image) {
        String exifDate = exifString(meta, ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        if (exifDate.length() >= 10) {
            // Format "yyyy:MM:dd ..." -> "yyyy-MM-dd"
            return exifDate.substring(0, 10).replace(':', '-');
        }
        if (image != null && image.exists()) {
            LocalDate d = Instant.ofEpochMilli(image.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
            return d.toString();
        }
        return "";
    }

    private static String gps(JpegImageMetadata meta) {
        if (meta == null) {
            return "";
        }
        try {
            TiffImageMetadata exif = meta.getExif();
            if (exif != null) {
                TiffImageMetadata.GpsInfo gpsInfo = exif.getGpsInfo();
                if (gpsInfo != null) {
                    return String.format(java.util.Locale.ROOT, "%.5f, %.5f", gpsInfo.getLatitudeAsDegreesNorth(),
                            gpsInfo.getLongitudeAsDegreesEast());
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "GPS nicht lesbar: " + ex.getMessage());
        }
        return "";
    }

    private static String exifString(JpegImageMetadata meta, TagInfo tag) {
        if (meta == null) {
            return "";
        }
        try {
            TiffField field = meta.findExifValueWithExactMatch(tag);
            if (field != null) {
                String value = field.getStringValue();
                return value == null ? "" : value.trim();
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "EXIF-Feld nicht lesbar (" + tag.name + "): " + ex.getMessage());
        }
        return "";
    }
}
