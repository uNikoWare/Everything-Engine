package com.everythingengine.desktop.testing;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record TestRunResult(
        Status status,
        int passed,
        int failed,
        int skipped,
        Duration duration,
        Instant completedAt,
        String message,
        String output) {
    public enum Status {
        SUCCESSFUL,
        FAILED,
        ERROR
    }

    public TestRunResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(completedAt, "completedAt");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(output, "output");
    }
}
