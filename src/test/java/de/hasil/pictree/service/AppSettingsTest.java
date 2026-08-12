package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.hasil.pictree.model.StampStyle;

class AppSettingsTest {

    @Test
    void defaultsAreSensible(@TempDir Path dir) {
        AppSettings s = new AppSettings(dir.resolve("settings.properties"));
        assertEquals(1000, s.getWindowWidth());
        assertEquals(680, s.getWindowHeight());
        assertEquals("", s.getLastFolder());
        assertEquals(0.92f, s.getJpegQuality(), 1e-6);
        assertEquals(AppSettings.THEME_LIGHT, s.getTheme());
        assertTrue(s.getTargetDir().replace('\\', '/').endsWith("Pictures/PicTreeAlbums"));
    }

    @Test
    void roundTripPersistsAllValues(@TempDir Path dir) {
        Path file = dir.resolve("settings.properties");
        AppSettings s = new AppSettings(file);
        s.setWindowSize(1234, 777);
        s.setLastFolder("D:/Bilder");
        s.setTargetDir("D:/Ausgabe");
        s.setJpegQuality(0.7f);
        s.setTheme(AppSettings.THEME_DARK);

        StampStyle style = new StampStyle();
        style.setColor(new Color(1, 2, 3));
        style.setFontFamily("Monospaced");
        style.setFontStyle(Font.BOLD);
        style.setRelativeSize(0.15f);
        style.setRelX(0.2);
        s.setDefaultStyle(style);
        s.save();

        AppSettings reloaded = new AppSettings(file).load();
        assertEquals(1234, reloaded.getWindowWidth());
        assertEquals(777, reloaded.getWindowHeight());
        assertEquals("D:/Bilder", reloaded.getLastFolder());
        assertEquals("D:/Ausgabe", reloaded.getTargetDir());
        assertEquals(0.7f, reloaded.getJpegQuality(), 1e-6);
        assertEquals(AppSettings.THEME_DARK, reloaded.getTheme());

        StampStyle back = reloaded.getDefaultStyle();
        assertEquals(new Color(1, 2, 3), back.getColor());
        assertEquals("Monospaced", back.getFontFamily());
        assertEquals(Font.BOLD, back.getFontStyle());
        assertEquals(0.15f, back.getRelativeSize(), 1e-6);
        assertEquals(0.2, back.getRelX(), 1e-9);
    }

    @Test
    void jpegQualityIsClamped(@TempDir Path dir) {
        AppSettings s = new AppSettings(dir.resolve("settings.properties"));
        s.setJpegQuality(5.0f);
        assertEquals(1.0f, s.getJpegQuality(), 1e-6);
        s.setJpegQuality(0.0f);
        assertEquals(0.1f, s.getJpegQuality(), 1e-6);
    }

    @Test
    void themeFallsBackToLightForUnknown(@TempDir Path dir) {
        AppSettings s = new AppSettings(dir.resolve("settings.properties"));
        s.setTheme("weirdo");
        assertEquals(AppSettings.THEME_LIGHT, s.getTheme());
    }
}
