package com.everythingengine.core.randomization;

import java.util.Objects;

/**
 * Compatibility wrapper for weighted random selection.
 */
public record WeightedChoice<T>(T value, double weight) {
    public WeightedChoice {
        Objects.requireNonNull(value, "value");
        if (Double.isNaN(weight) || Double.isInfinite(weight) || weight < 0d) {
            throw new IllegalArgumentException("Weight must be a finite, non-negative number");
        }
    }

    public static <T> WeightedChoice<T> of(T value, double weight) {
        return new WeightedChoice<>(value, weight);
    }

    public com.everythingengine.core.random.WeightedChoice<T> delegate() {
        return new com.everythingengine.core.random.WeightedChoice<>(value, weight);
    }
}
