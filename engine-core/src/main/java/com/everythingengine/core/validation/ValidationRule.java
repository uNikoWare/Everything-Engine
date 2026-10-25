package com.everythingengine.core.validation;

import java.util.Objects;

/**
 * A validation rule for a specific value.
 */
@FunctionalInterface
public interface ValidationRule<T> {
    ValidationResult validate(T value);

    static <T> ValidationRule<T> combine(Iterable<ValidationRule<T>> rules) {
        Objects.requireNonNull(rules, "rules");
        return value -> {
            ValidationResult aggregate = ValidationResult.valid();
            for (ValidationRule<T> rule : rules) {
                aggregate = aggregate.merge(Objects.requireNonNull(rule, "rule").validate(value));
            }
            return aggregate;
        };
    }
}
