package de.hasil.pictree.model;

/**
 * Persistierte Annotation eines Bildes: Kommentar-/Stempeltext samt Stil und
 * relativer Position.
 */
public record Annotation(String comment, StampStyle style) {
}
