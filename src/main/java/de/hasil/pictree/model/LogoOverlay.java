package de.hasil.pictree.model;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Ein optionales Logo-/Wasserzeichen-Overlay: Bild plus relative Position
 * (0..1, Ankermitte), relative Breite (Bruchteil der Bildbreite) und Deckkraft.
 */
public class LogoOverlay {

    private final File file;
    private final BufferedImage image;
    private double relX = 0.85;
    private double relY = 0.85;
    private double relWidthFraction = 0.2;
    private float opacity = 0.7f;

    public LogoOverlay(File file, BufferedImage image) {
        this.file = file;
        this.image = image;
    }

    public File getFile() {
        return file;
    }

    public BufferedImage getImage() {
        return image;
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

    public double getRelWidthFraction() {
        return relWidthFraction;
    }

    public void setRelWidthFraction(double f) {
        this.relWidthFraction = Math.max(0.01, Math.min(1.0, f));
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity < 0 ? 0 : (opacity > 1 ? 1 : opacity);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
