package com.everythingengine.core.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated validation outcome containing both errors and warnings.
 */
public final class ValidationResult {
    private static final ValidationResult VALID = new ValidationResult(List.of(), List.of());

    private final List<ValidationIssue> errors;
    private final List<ValidationIssue> warnings;

    public ValidationResult(List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        this.errors = List.copyOf(Objects.requireNonNullElse(errors, List.of()));
        this.warnings = List.copyOf(Objects.requireNonNullElse(warnings, List.of()));
    }

    public static ValidationResult valid() {
        return VALID;
    }

    public static ValidationResult of(List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        return new ValidationResult(errors, warnings);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<ValidationIssue> errors() {
        return errors;
    }

    public List<ValidationIssue> warnings() {
        return warnings;
    }

    public ValidationResult merge(ValidationResult other) {
        Objects.requireNonNull(other, "other");
        List<ValidationIssue> mergedErrors = new ArrayList<>(this.errors);
        mergedErrors.addAll(other.errors);
        List<ValidationIssue> mergedWarnings = new ArrayList<>(this.warnings);
        mergedWarnings.addAll(other.warnings);
        return new ValidationResult(mergedErrors, mergedWarnings);
    }

    public void assertValid() {
        if (!isValid()) {
            throw new IllegalArgumentException(errors.getFirst().message());
        }
    }
}
