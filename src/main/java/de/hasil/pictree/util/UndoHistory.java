package de.hasil.pictree.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Generische Undo/Redo-Historie über unveränderlichen Zustands-Schnappschüssen.
 * Ein Zeiger markiert den aktuellen Zustand; {@link #record} verwirft eine
 * eventuelle Redo-Kette. Die Größe ist begrenzt (älteste Einträge fallen weg).
 */
public class UndoHistory<T> {

    private final List<T> states = new ArrayList<>();
    private final int limit;
    private int index = -1;

    public UndoHistory() {
        this(100);
    }

    public UndoHistory(int limit) {
        this.limit = Math.max(2, limit);
    }

    /** Setzt die Historie mit einem Startzustand zurück. */
    public void reset(T initial) {
        states.clear();
        states.add(initial);
        index = 0;
    }

    /** Nimmt einen neuen Zustand auf (verwirft die Redo-Kette). */
    public void record(T state) {
        if (index < 0) {
            reset(state);
            return;
        }
        while (states.size() > index + 1) {
            states.remove(states.size() - 1);
        }
        states.add(state);
        index++;
        while (states.size() > limit) {
            states.remove(0);
            index--;
        }
    }

    public boolean canUndo() {
        return index > 0;
    }

    public boolean canRedo() {
        return index >= 0 && index < states.size() - 1;
    }

    /** Geht einen Schritt zurück und liefert den dann aktuellen Zustand (oder null). */
    public T undo() {
        if (!canUndo()) {
            return null;
        }
        index--;
        return states.get(index);
    }

    /** Geht einen Schritt vor und liefert den dann aktuellen Zustand (oder null). */
    public T redo() {
        if (!canRedo()) {
            return null;
        }
        index++;
        return states.get(index);
    }

    public T current() {
        return index >= 0 ? states.get(index) : null;
    }

    public int size() {
        return states.size();
    }
}
