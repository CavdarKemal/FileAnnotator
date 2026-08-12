package de.hasil.pictree.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class LoggingTest {

    @Test
    void getReturnsNamedLoggerAndIsIdempotent() {
        Logger a = Logging.get(LoggingTest.class);
        Logger b = Logging.get(LoggingTest.class);
        assertNotNull(a);
        assertEquals(LoggingTest.class.getName(), a.getName());
        assertSame(a, b); // java.util.logging cached denselben Logger
    }

    @Test
    void compactFormatterContainsLevelShortNameAndMessage() {
        Logging.CompactFormatter formatter = new Logging.CompactFormatter();
        LogRecord record = new LogRecord(Level.WARNING, "Testnachricht");
        record.setLoggerName("de.hasil.pictree.service.ExifService");

        String out = formatter.format(record);
        assertTrue(out.contains("[WARNING]"), out);
        assertTrue(out.contains("ExifService:"), out);
        assertTrue(out.contains("Testnachricht"), out);
    }

    @Test
    void formatterAppendsThrowable() {
        Logging.CompactFormatter formatter = new Logging.CompactFormatter();
        LogRecord record = new LogRecord(Level.SEVERE, "mit Fehler");
        record.setLoggerName("X");
        record.setThrown(new IllegalStateException("boom"));
        String out = formatter.format(record);
        assertTrue(out.contains("IllegalStateException"), out);
    }
}
