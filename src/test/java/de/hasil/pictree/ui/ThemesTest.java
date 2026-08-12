package de.hasil.pictree.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.hasil.pictree.service.AppSettings;

class ThemesTest {

    @Test
    void applyDarkActivatesFlatLaf() {
        assertTrue(Themes.apply(AppSettings.THEME_DARK));
        assertTrue(Themes.isFlatLafActive());
    }

    @Test
    void applyLightActivatesFlatLaf() {
        assertTrue(Themes.apply(AppSettings.THEME_LIGHT));
        assertTrue(Themes.isFlatLafActive());
    }

    @Test
    void unknownThemeStillApplies() {
        assertTrue(Themes.apply("irgendwas"));
        assertTrue(Themes.isFlatLafActive());
    }
}
