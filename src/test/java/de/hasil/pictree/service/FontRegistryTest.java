package de.hasil.pictree.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;

import org.junit.jupiter.api.Test;

class FontRegistryTest {

    @Test
    void robotoResourceIsBundledAndAvailable() {
        assertTrue(FontRegistry.isRobotoAvailable(), "Roboto-Regular.ttf muss als Resource gebündelt sein");
    }

    @Test
    void resolveRobotoReturnsRobotoFamily() {
        Font f = FontRegistry.resolve(FontRegistry.ROBOTO, Font.BOLD, 48);
        assertNotNull(f);
        assertEquals("Roboto", f.getFamily());
        assertEquals(48f, f.getSize2D(), 0.01);
        assertTrue(f.isBold());
    }

    @Test
    void resolveUnknownFamilyFallsBackWithoutError() {
        Font f = FontRegistry.resolve("GibtEsNichtXYZ", Font.PLAIN, 12);
        assertNotNull(f); // Java liefert einen Default-Font, kein null
    }

    @Test
    void availableFamiliesContainRobotoAndMonospaced() {
        assertTrue(FontRegistry.AVAILABLE_FAMILIES.contains("Roboto"));
        assertTrue(FontRegistry.AVAILABLE_FAMILIES.contains("Monospaced"));
    }
}
