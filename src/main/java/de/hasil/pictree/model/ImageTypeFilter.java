package de.hasil.pictree.model;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Immutabler Bildtyp-Filter: kapselt, welche Bild-<em>Gruppen</em> (JPEG, PNG,
 * …) aktiv sind, und wirkt daraus abgeleitet als Datei-Prädikat. Ein einziger
 * Filter-Zustand steuert Explorer-Baum, Thumbnail-Streifen und Stapel-Auswahl,
 * damit die Endungsmenge überall konsistent ist.
 *
 * <p>Die Vereinigung aller Gruppen entspricht den lesbaren Bildformaten aus
 * {@link de.hasil.pictree.service.ImageSupport}.
 */
public final class ImageTypeFilter {

    /** Gruppen-Anzeigename → zugehörige (kleingeschriebene) Endungen, Reihenfolge stabil. */
    public static final Map<String, Set<String>> GROUPS;

    static {
        Map<String, Set<String>> g = new LinkedHashMap<>();
        g.put("JPEG", Set.of("jpg", "jpeg"));
        g.put("PNG", Set.of("png"));
        g.put("GIF", Set.of("gif"));
        g.put("BMP", Set.of("bmp", "wbmp"));
        g.put("TIFF", Set.of("tif", "tiff"));
        GROUPS = Collections.unmodifiableMap(g);
    }

    /** Aktive Gruppennamen (Teilmenge der Schlüssel von {@link #GROUPS}). */
    private final Set<String> activeGroups;

    private ImageTypeFilter(Set<String> activeGroups) {
        // Nur bekannte Gruppen, Reihenfolge gemäß GROUPS beibehalten.
        Set<String> filtered = new LinkedHashSet<>();
        for (String group : GROUPS.keySet()) {
            if (activeGroups.contains(group)) {
                filtered.add(group);
            }
        }
        this.activeGroups = Collections.unmodifiableSet(filtered);
    }

    /** Filter mit allen Gruppen aktiv (Standard – zeigt alle unterstützten Bildtypen). */
    public static ImageTypeFilter all() {
        return new ImageTypeFilter(new LinkedHashSet<>(GROUPS.keySet()));
    }

    /** Liefert eine Kopie mit {@code group} an- bzw. abgeschaltet. */
    public ImageTypeFilter withGroup(String group, boolean on) {
        if (!GROUPS.containsKey(group)) {
            return this;
        }
        Set<String> next = new LinkedHashSet<>(activeGroups);
        if (on) {
            next.add(group);
        } else {
            next.remove(group);
        }
        return new ImageTypeFilter(next);
    }

    public boolean isGroupActive(String group) {
        return activeGroups.contains(group);
    }

    /** Alle aktiven Gruppen aktiv? (Grundlage für die „Alle Bilder"-Checkbox.) */
    public boolean isAllActive() {
        return activeGroups.size() == GROUPS.size();
    }

    /** Keine Gruppe aktiv → es werden keine Bilddateien akzeptiert. */
    public boolean isEmpty() {
        return activeGroups.isEmpty();
    }

    /** Vereinigung der Endungen aller aktiven Gruppen (kleingeschrieben, ohne Punkt). */
    public Set<String> extensions() {
        Set<String> exts = new LinkedHashSet<>();
        for (String group : activeGroups) {
            exts.addAll(GROUPS.get(group));
        }
        return Collections.unmodifiableSet(exts);
    }

    /** Ist {@code f} eine Bilddatei eines aktiven Typs? */
    public boolean acceptsImage(File f) {
        if (f == null || !f.isFile()) {
            return false;
        }
        return extensions().contains(extensionOf(f.getName()));
    }

    /** Baum-Prädikat: Verzeichnisse immer, Dateien nur bei passendem Bildtyp. */
    public boolean acceptsInTree(File f) {
        if (f == null) {
            return false;
        }
        return f.isDirectory() || acceptsImage(f);
    }

    /** Serialisiert die aktiven Gruppen als kommaseparierte Liste (für {@code AppSettings}). */
    public String encode() {
        return String.join(",", activeGroups);
    }

    /**
     * Liest einen zuvor mit {@link #encode()} erzeugten Wert. Leer/{@code null}
     * ergibt {@link #all()} (sinnvoller Standard).
     */
    public static ImageTypeFilter decode(String s) {
        if (s == null || s.isBlank()) {
            return all();
        }
        Set<String> groups = new LinkedHashSet<>();
        for (String part : s.split(",")) {
            String name = part.trim();
            if (GROUPS.containsKey(name)) {
                groups.add(name);
            }
        }
        return groups.isEmpty() ? all() : new ImageTypeFilter(groups);
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ImageTypeFilter other && activeGroups.equals(other.activeGroups);
    }

    @Override
    public int hashCode() {
        return activeGroups.hashCode();
    }

    @Override
    public String toString() {
        return "ImageTypeFilter" + activeGroups;
    }
}
