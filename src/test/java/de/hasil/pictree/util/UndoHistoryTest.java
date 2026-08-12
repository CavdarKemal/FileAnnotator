package de.hasil.pictree.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UndoHistoryTest {

    @Test
    void undoRedoWalksStates() {
        UndoHistory<String> h = new UndoHistory<>();
        h.reset("a");
        h.record("b");
        h.record("c");

        assertTrue(h.canUndo());
        assertFalse(h.canRedo());
        assertEquals("c", h.current());

        assertEquals("b", h.undo());
        assertEquals("a", h.undo());
        assertFalse(h.canUndo());
        assertNull(h.undo());

        assertEquals("b", h.redo());
        assertEquals("c", h.redo());
        assertFalse(h.canRedo());
        assertNull(h.redo());
    }

    @Test
    void recordAfterUndoTruncatesRedoTail() {
        UndoHistory<String> h = new UndoHistory<>();
        h.reset("a");
        h.record("b");
        h.record("c");
        h.undo();          // -> b
        h.record("x");     // Redo-Kette (c) wird verworfen
        assertEquals("x", h.current());
        assertFalse(h.canRedo());
        assertEquals("b", h.undo());
    }

    @Test
    void recordWithoutResetInitializes() {
        UndoHistory<String> h = new UndoHistory<>();
        h.record("first");
        assertEquals("first", h.current());
        assertFalse(h.canUndo());
    }

    @Test
    void limitDropsOldestStates() {
        UndoHistory<Integer> h = new UndoHistory<>(3);
        h.reset(0);
        h.record(1);
        h.record(2);
        h.record(3); // Kapazität 3 -> ältester (0) faellt weg
        assertEquals(3, h.size());
        assertEquals(3, h.current());
        // Nur bis zum aeltesten verbliebenen zurueck.
        assertEquals(2, h.undo());
        assertEquals(1, h.undo());
        assertFalse(h.canUndo());
    }
}
