package de.hasil.pictree.model;

/**
 * Unveränderlicher Schnappschuss des bearbeitbaren Zustands (Kommentartext +
 * Stil/Position) für die Undo/Redo-Historie.
 */
public record EditState(String comment, StampStyle style) {

    /** Erzeugt einen Schnappschuss mit einer Kopie des Stils. */
    public static EditState of(String comment, StampStyle style) {
        return new EditState(comment == null ? "" : comment, style.copy());
    }
}
