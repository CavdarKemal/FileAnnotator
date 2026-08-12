package de.hasil.pictree.model;

import java.awt.Color;
import java.awt.Font;

/**
 * Stil und Position des Text-Stempels. Größe und Position werden <b>relativ</b>
 * zur Bildfläche gespeichert (Bruchteile 0..1), damit dieselbe Annotation
 * unabhängig von Skalierung und Bildauflösung identisch wirkt – Grundlage für
 * WYSIWYG-Vorschau und Stapelverarbeitung über unterschiedlich große Bilder.
 */
public class StampStyle {

    /** Textfarbe. */
    private Color color = Color.WHITE;

    /** Schriftfamilie, z. B. "Roboto", "Monospaced", "SansSerif". */
    private String fontFamily = "SansSerif";

    /** AWT-Font-Stil ({@link Font#PLAIN}, {@link Font#BOLD} …). */
    private int fontStyle = Font.BOLD;

    /** Schriftgröße als Bruchteil der Bildhöhe (z. B. 0.06 = 6 %). */
    private float relativeSize = 0.06f;

    /** Horizontaler Anker (Mitte des Textblocks), 0..1 der Bildbreite. */
    private double relX = 0.5;

    /** Vertikaler Anker (Mitte des Textblocks), 0..1 der Bildhöhe. */
    private double relY = 0.88;

    /** Dezenter Umriss/Schatten für Lesbarkeit auf beliebigem Hintergrund. */
    private boolean outline = true;

    /** Drehwinkel des Textblocks in Grad (im Uhrzeigersinn). */
    private double rotationDegrees = 0.0;

    /** Umbruchbreite als Bruchteil der Bildbreite (0 = kein automatischer Umbruch). */
    private double wrapWidthFraction = 0.0;

    public StampStyle() {
        // Standardwerte
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(int fontStyle) {
        this.fontStyle = fontStyle;
    }

    public float getRelativeSize() {
        return relativeSize;
    }

    public void setRelativeSize(float relativeSize) {
        this.relativeSize = relativeSize;
    }

    public double getRelX() {
        return relX;
    }

    public void setRelX(double relX) {
        this.relX = clamp01(relX);
    }

    public double getRelY() {
        return relY;
    }

    public void setRelY(double relY) {
        this.relY = clamp01(relY);
    }

    public boolean isOutline() {
        return outline;
    }

    public void setOutline(boolean outline) {
        this.outline = outline;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public double getWrapWidthFraction() {
        return wrapWidthFraction;
    }

    public void setWrapWidthFraction(double wrapWidthFraction) {
        this.wrapWidthFraction = clamp01(wrapWidthFraction);
    }

    /** Übernimmt alle Werte aus {@code o} in dieses Objekt (In-Place). */
    public void copyFrom(StampStyle o) {
        if (o == null) {
            return;
        }
        this.color = o.color;
        this.fontFamily = o.fontFamily;
        this.fontStyle = o.fontStyle;
        this.relativeSize = o.relativeSize;
        this.relX = o.relX;
        this.relY = o.relY;
        this.outline = o.outline;
        this.rotationDegrees = o.rotationDegrees;
        this.wrapWidthFraction = o.wrapWidthFraction;
    }

    /** Tiefe Kopie dieses Stils. */
    public StampStyle copy() {
        StampStyle c = new StampStyle();
        c.color = this.color;
        c.fontFamily = this.fontFamily;
        c.fontStyle = this.fontStyle;
        c.relativeSize = this.relativeSize;
        c.relX = this.relX;
        c.relY = this.relY;
        c.outline = this.outline;
        c.rotationDegrees = this.rotationDegrees;
        c.wrapWidthFraction = this.wrapWidthFraction;
        return c;
    }

    private static double clamp01(double v) {
        if (v < 0) {
            return 0;
        }
        if (v > 1) {
            return 1;
        }
        return v;
    }
}
