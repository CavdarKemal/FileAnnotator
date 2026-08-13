package de.hasil.pictree.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Zentrale Logging-Konfiguration auf Basis von {@code java.util.logging}.
 * Einmalige Initialisierung eines kompakten Konsolen-Formats; Zugriff über
 * {@link #get(Class)}.
 */
public final class Logging {

    private static volatile boolean initialized;

    private Logging() {
    }

    /** Konfiguriert den Root-Logger einmalig. */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) {
            root.removeHandler(h);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new CompactFormatter());
        root.addHandler(handler);
        root.setLevel(Level.INFO);
        initialized = true;
    }

    /** Liefert einen Logger für die angegebene Klasse (initialisiert bei Bedarf). */
    public static Logger get(Class<?> type) {
        init();
        return Logger.getLogger(type.getName());
    }

    /** Kompaktes, einzeiliges Log-Format: {@code [LEVEL] kurzerName: Nachricht}. */
    static final class CompactFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String loggerName = record.getLoggerName() == null ? "" : record.getLoggerName();
            int dot = loggerName.lastIndexOf('.');
            String shortName = dot >= 0 ? loggerName.substring(dot + 1) : loggerName;
            StringBuilder sb = new StringBuilder().append('[').append(record.getLevel()).append("] ")
                    .append(shortName).append(": ").append(formatMessage(record)).append(System.lineSeparator());
            if (record.getThrown() != null) {
                sb.append(record.getThrown()).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }
}
