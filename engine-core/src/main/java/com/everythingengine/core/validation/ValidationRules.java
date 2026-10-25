package com.everythingengine.core.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Simple reusable validation rules for common configuration checks.
 */
public final class ValidationRules {
    private ValidationRules() {
    }

    public static <T> ValidationRule<T> requiredField(String field, Function<T, ?> accessor, String message) {
        return value -> {
            Object resolved = accessor.apply(value);
            if (resolved == null) {
                return invalid(field, message, "required");
            }
            return ValidationResult.valid();
        };
    }

    public static <T> ValidationRule<T> nonBlankString(String field, Function<T, String> accessor, String message) {
        return value -> {
            String resolved = accessor.apply(value);
            if (resolved == null || resolved.isBlank()) {
                return invalid(field, message, "blank");
            }
            return ValidationResult.valid();
        };
    }

    public static <T> ValidationRule<T> lengthBetween(
            String field,
            Function<T, String> accessor,
            int minLength,
            int maxLength,
            String message) {
        return value -> {
            String resolved = accessor.apply(value);
            if (resolved == null) {
                return ValidationResult.valid();
            }
            if (resolved.length() < minLength || resolved.length() > maxLength) {
                return invalid(field, message, "length");
            }
            return ValidationResult.valid();
        };
    }

    public static <T> ValidationRule<T> numericRange(
            String field,
            Function<T, Number> accessor,
            double min,
            double max,
            String message) {
        return value -> {
            Number resolved = accessor.apply(value);
            if (resolved == null) {
                return ValidationResult.valid();
            }
            double number = resolved.doubleValue();
            if (number < min || number > max) {
                return invalid(field, message, "range");
            }
            return ValidationResult.valid();
        };
    }

    public static <T> ValidationRule<T> allowedValues(
            String field,
            Function<T, ?> accessor,
            Collection<?> allowedValues,
            String message) {
        return value -> {
            Object resolved = accessor.apply(value);
            if (resolved == null) {
                return ValidationResult.valid();
            }
            if (!allowedValues.contains(resolved)) {
                return invalid(field, message, "allowed-values");
            }
            return ValidationResult.valid();
        };
    }

    public static <T> ValidationRule<T> nonEmptyCollection(
            String field,
            Function<T, ? extends Collection<?>> accessor,
            String message) {
        return value -> {
            Collection<?> resolved = accessor.apply(value);
            if (resolved == null || resolved.isEmpty()) {
                return invalid(field, message, "empty");
            }
            return ValidationResult.valid();
        };
    }

    public static <T> ValidationRule<T> custom(String field, Function<T, String> messageFactory) {
        return value -> {
            String message = messageFactory.apply(value);
            if (message == null || message.isBlank()) {
                return ValidationResult.valid();
            }
            return invalid(field, message, "custom");
        };
    }

    public static <T> ValidationResult combine(T value, Iterable<ValidationRule<T>> rules) {
        ValidationResult aggregate = ValidationResult.valid();
        for (ValidationRule<T> rule : rules) {
            aggregate = aggregate.merge(Objects.requireNonNull(rule, "rule").validate(value));
        }
        return aggregate;
    }

    public static ValidationResult invalid(String field, String message, String code) {
        return new ValidationResult(List.of(new ValidationIssue(field, message, code)), List.of());
    }
}
