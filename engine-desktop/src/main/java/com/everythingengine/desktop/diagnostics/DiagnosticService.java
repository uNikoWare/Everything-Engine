package com.everythingengine.desktop.diagnostics;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class DiagnosticService {
    private final List<DiagnosticEntry> entries = new CopyOnWriteArrayList<>();
    private final Logger logger = Logger.getLogger("com.everythingengine.desktop");

    public DiagnosticService() {
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record != null && isLoggable(record)) {
                    String message = record.getMessage();
                    int sourceEnd = message.indexOf("] ");
                    String source = record.getLoggerName();
                    if (message.startsWith("[") && sourceEnd > 1) {
                        source = message.substring(1, sourceEnd);
                        message = message.substring(sourceEnd + 2);
                    }
                    if (record.getThrown() != null) {
                        Throwable error = record.getThrown();
                        message += " (" + error.getClass().getSimpleName()
                                + (error.getMessage() == null ? "" : ": " + error.getMessage()) + ")";
                    }
                    entries.add(new DiagnosticEntry(
                            record.getInstant(),
                            record.getLevel(),
                            source,
                            message));
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    public void info(String source, String message) {
        logger.info(() -> format(source, message));
    }

    public void warning(String source, String message) {
        logger.warning(() -> format(source, message));
    }

    public void error(String source, String message, Throwable error) {
        logger.log(java.util.logging.Level.SEVERE, format(source, message), error);
    }

    public List<DiagnosticEntry> entries() {
        return List.copyOf(entries);
    }

    public long errorCount() {
        return entries.stream().filter(entry -> entry.level().intValue() >= Level.SEVERE.intValue()).count();
    }

    private String format(String source, String message) {
        return "[" + source + "] " + message;
    }
}
