package com.everythingengine.desktop.diagnostics;

import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;

public record DiagnosticEntry(Instant timestamp, Level level, String source, String message) {
    public DiagnosticEntry {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(message, "message");
    }
}
