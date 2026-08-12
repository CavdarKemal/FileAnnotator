package de.hasil.pictree.service;

import java.awt.Color;
import java.util.Properties;

import de.hasil.pictree.model.StampStyle;

/**
 * (De-)Serialisiert einen {@link StampStyle} in/aus {@link Properties}, mit
 * konfigurierbarem Schlüssel-Präfix. Gemeinsam genutzt von
 * {@link AnnotationStore} (Präfix "") und der App-Konfiguration.
 */
public final class StampStyleCodec {

    private StampStyleCodec() {
    }

    /** Schreibt alle Style-Felder unter {@code prefix} in {@code p}. */
    public static void write(Properties p, String prefix, StampStyle s) {
        p.setProperty(prefix + "color", Integer.toString(s.getColor().getRGB()));
        p.setProperty(prefix + "fontFamily", s.getFontFamily());
        p.setProperty(prefix + "fontStyle", Integer.toString(s.getFontStyle()));
        p.setProperty(prefix + "relativeSize", Float.toString(s.getRelativeSize()));
        p.setProperty(prefix + "relX", Double.toString(s.getRelX()));
        p.setProperty(prefix + "relY", Double.toString(s.getRelY()));
        p.setProperty(prefix + "outline", Boolean.toString(s.isOutline()));
        p.setProperty(prefix + "rotationDegrees", Double.toString(s.getRotationDegrees()));
        p.setProperty(prefix + "wrapWidthFraction", Double.toString(s.getWrapWidthFraction()));
    }

    /** Liest einen Style unter {@code prefix}; fehlende Felder werden aus {@code fallback} übernommen. */
    public static StampStyle read(Properties p, String prefix, StampStyle fallback) {
        StampStyle base = fallback == null ? new StampStyle() : fallback.copy();
        StampStyle s = new StampStyle();
        s.copyFrom(base);
        s.setColor(new Color(parseInt(p, prefix + "color", base.getColor().getRGB()), true));
        s.setFontFamily(p.getProperty(prefix + "fontFamily", base.getFontFamily()));
        s.setFontStyle(parseInt(p, prefix + "fontStyle", base.getFontStyle()));
        s.setRelativeSize((float) parseDouble(p, prefix + "relativeSize", base.getRelativeSize()));
        s.setRelX(parseDouble(p, prefix + "relX", base.getRelX()));
        s.setRelY(parseDouble(p, prefix + "relY", base.getRelY()));
        s.setOutline(Boolean.parseBoolean(
                p.getProperty(prefix + "outline", Boolean.toString(base.isOutline()))));
        s.setRotationDegrees(parseDouble(p, prefix + "rotationDegrees", base.getRotationDegrees()));
        s.setWrapWidthFraction(parseDouble(p, prefix + "wrapWidthFraction", base.getWrapWidthFraction()));
        return s;
    }

    static int parseInt(Properties p, String key, int fallback) {
        try {
            String v = p.getProperty(key);
            return v == null ? fallback : Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    static double parseDouble(Properties p, String key, double fallback) {
        try {
            String v = p.getProperty(key);
            return v == null ? fallback : Double.parseDouble(v.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
