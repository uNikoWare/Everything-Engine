package com.everythingengine.core.validation;

import java.util.Objects;

/**
 * A single validation finding tied to a field, message, and optional code.
 */
public record ValidationIssue(String field, String message, String code) {
    public ValidationIssue {
        Objects.requireNonNull(message, "message");
        if (field != null && field.isBlank()) {
            throw new IllegalArgumentException("Field must not be blank");
        }
        if (code != null && code.isBlank()) {
            code = null;
        }
    }

    public static ValidationIssue warning(String field, String message) {
        return new ValidationIssue(field, message, null);
    }

    public static ValidationIssue error(String field, String message, String code) {
        return new ValidationIssue(field, message, code);
    }
}
