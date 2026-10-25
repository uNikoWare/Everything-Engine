package com.everythingengine.core.random;

import java.util.Objects;

/**
 * A value paired with a non-negative weight used by weighted random selection.
 */
public record WeightedChoice<T>(T value, double weight) {
    public WeightedChoice {
        Objects.requireNonNull(value, "value");
        if (Double.isNaN(weight) || Double.isInfinite(weight)) {
            throw new IllegalArgumentException("Weight must be a finite number");
        }
        if (weight < 0d) {
            throw new IllegalArgumentException("Weight must not be negative");
        }
    }

    public static <T> WeightedChoice<T> of(T value, double weight) {
        return new WeightedChoice<>(value, weight);
    }
}
