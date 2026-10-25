package com.everythingengine.core.randomization;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Compatibility wrapper around the primary seeded random implementation.
 */
public final class SeededRandom {
    private final com.everythingengine.core.random.SeededRandom delegate;

    public SeededRandom() {
        this.delegate = new com.everythingengine.core.random.SeededRandom();
    }

    public SeededRandom(long seed) {
        this.delegate = new com.everythingengine.core.random.SeededRandom(seed);
    }

    public static SeededRandom create() {
        return new SeededRandom();
    }

    public static SeededRandom create(long seed) {
        return new SeededRandom(seed);
    }

    public static SeededRandom fromSeed(long seed) {
        return new SeededRandom(seed);
    }

    public long seed() {
        return delegate.seed();
    }

    public SeededRandom split() {
        return new SeededRandom(delegate.split().seed());
    }

    public int nextInt(int minInclusive, int maxInclusive) {
        return delegate.nextInt(minInclusive, maxInclusive);
    }

    public long nextLong(long minInclusive, long maxInclusive) {
        return delegate.nextLong(minInclusive, maxInclusive);
    }

    public double nextDouble(double minInclusive, double maxExclusive) {
        return delegate.nextDouble(minInclusive, maxExclusive);
    }

    public boolean nextBoolean() {
        return delegate.nextBoolean();
    }

    public boolean nextBoolean(double probabilityPercent) {
        return delegate.nextBoolean(probabilityPercent);
    }

    public <T> T choose(Collection<T> values) {
        return delegate.choose(values);
    }

    public <T> T weightedChoice(Collection<WeightedChoice<T>> options) {
        return delegate.weightedChoice(options.stream()
                .map(option -> new com.everythingengine.core.random.WeightedChoice<>(option.value(), option.weight()))
                .toList());
    }

    public <T> T weightedChoice(Map<T, Number> weights) {
        return delegate.weightedChoice(weights);
    }

    public com.everythingengine.core.random.SeededRandom delegate() {
        return delegate;
    }
}
