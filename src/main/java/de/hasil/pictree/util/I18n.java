package de.hasil.pictree.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Einfache Internationalisierung über {@link ResourceBundle}. Das Basis-Bundle
 * ({@code i18n/messages.properties}) ist Deutsch; {@code messages_en.properties}
 * liefert Englisch. Fehlt ein Schlüssel, wird der Schlüssel selbst zurückgegeben.
 */
public final class I18n {

    private static final String BASE = "i18n.messages";
    private static volatile ResourceBundle bundle = load(Locale.getDefault());

    private I18n() {
    }

    /** Setzt die aktive Sprache. */
    public static void init(Locale locale) {
        bundle = load(locale);
    }

    private static ResourceBundle load(Locale locale) {
        try {
            return ResourceBundle.getBundle(BASE, locale);
        } catch (MissingResourceException ex) {
            return ResourceBundle.getBundle(BASE, Locale.ROOT);
        }
    }

    /** Übersetzt {@code key}; unbekannte Schlüssel werden unverändert zurückgegeben. */
    public static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    /** Übersetzt mit {@link MessageFormat}-Argumenten. */
    public static String t(String key, Object... args) {
        return MessageFormat.format(t(key), args);
    }
}
