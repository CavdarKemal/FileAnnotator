package de.hasil.pictree.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class I18nTest {

    @AfterEach
    void reset() {
        I18n.init(Locale.GERMAN);
    }

    @Test
    void germanIsDefaultBundle() {
        I18n.init(Locale.GERMAN);
        assertEquals("Datei", I18n.t("menu.file"));
        assertEquals("Bild speichern", I18n.t("button.save"));
    }

    @Test
    void englishBundleIsUsed() {
        I18n.init(Locale.ENGLISH);
        assertEquals("File", I18n.t("menu.file"));
        assertEquals("Save image", I18n.t("button.save"));
    }

    @Test
    void unknownKeyReturnsKeyItself() {
        I18n.init(Locale.ENGLISH);
        assertEquals("does.not.exist", I18n.t("does.not.exist"));
    }

    @Test
    void unknownLocaleFallsBackToDefault() {
        I18n.init(Locale.forLanguageTag("xx"));
        // Fällt auf Basis-Bundle (Deutsch) zurück.
        assertEquals("Datei", I18n.t("menu.file"));
    }
}
