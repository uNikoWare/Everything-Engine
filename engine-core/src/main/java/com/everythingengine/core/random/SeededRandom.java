package com.everythingengine.core.random;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A deterministic random source that avoids shared global state.
 *
 * <p>Range semantics: integers are inclusive on both ends, while doubles are inclusive at the start and
 * exclusive at the end, matching the usual Java {@link java.util.Random} semantics.
 */
public final class SeededRandom {
    private final SplittableRandom delegate;
    private final long seed;

    public SeededRandom() {
        this(ThreadLocalRandom.current().nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    public SeededRandom(long seed) {
        this.seed = seed;
        this.delegate = new SplittableRandom(seed);
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
        return seed;
    }

    public SeededRandom split() {
        long derivedSeed = seed ^ Long.rotateLeft(delegate.nextLong(), 1) ^ 0x9E3779B97F4A7C15L;
        return new SeededRandom(derivedSeed);
    }

    public int nextInt(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("Minimum must not be greater than maximum");
        }
        if (minInclusive == maxInclusive) {
            return minInclusive;
        }
        return delegate.nextInt(minInclusive, maxInclusive + 1);
    }

    public long nextLong(long minInclusive, long maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("Minimum must not be greater than maximum");
        }
        if (minInclusive == maxInclusive) {
            return minInclusive;
        }
        long span = maxInclusive - minInclusive + 1L;
        return minInclusive + Math.floorMod(delegate.nextLong(), span);
    }

    public double nextDouble(double minInclusive, double maxExclusive) {
        if (Double.isNaN(minInclusive) || Double.isNaN(maxExclusive) || Double.isInfinite(minInclusive)
                || Double.isInfinite(maxExclusive)) {
            throw new IllegalArgumentException("Range bounds must be finite");
        }
        if (minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("Max must be greater than min");
        }
        double span = maxExclusive - minInclusive;
        return minInclusive + delegate.nextDouble() * span;
    }

    public boolean nextBoolean() {
        return delegate.nextBoolean();
    }

    public boolean nextBoolean(double probabilityPercent) {
        if (probabilityPercent < 0d || probabilityPercent > 100d) {
            throw new IllegalArgumentException("Probability must be between 0 and 100 percent");
        }
        return delegate.nextDouble(0d, 100d) < probabilityPercent;
    }

    public <T> T choose(Collection<T> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Selection source must not be empty");
        }
        int index = delegate.nextInt(values.size());
        if (values instanceof java.util.List<T> list) {
            return list.get(index);
        }
        int current = 0;
        for (T value : values) {
            if (current == index) {
                return value;
            }
            current++;
        }
        throw new IllegalStateException("Unable to select an element from the provided collection");
    }

    public <T> T weightedChoice(Collection<WeightedChoice<T>> options) {
        Objects.requireNonNull(options, "options");
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Weighted options must not be empty");
        }

        double totalWeight = 0d;
        for (WeightedChoice<T> option : options) {
            if (option.weight() <= 0d) {
                continue;
            }
            totalWeight += option.weight();
        }
        if (totalWeight <= 0d) {
            throw new IllegalArgumentException("Weighted options must contain at least one positive weight");
        }

        double threshold = delegate.nextDouble() * totalWeight;
        double accumulated = 0d;
        for (WeightedChoice<T> option : options) {
            if (option.weight() <= 0d) {
                continue;
            }
            accumulated += option.weight();
            if (threshold < accumulated) {
                return option.value();
            }
        }
        return options.iterator().next().value();
    }

    public <T> T weightedChoice(Map<T, Number> weights) {
        Objects.requireNonNull(weights, "weights");
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("Weight map must not be empty");
        }

        Map<T, WeightedChoice<T>> adapted = new LinkedHashMap<>();
        for (Map.Entry<T, Number> entry : weights.entrySet()) {
            double weightValue = requireWeight(entry.getValue());
            if (weightValue > 0d) {
                adapted.put(entry.getKey(), WeightedChoice.of(entry.getKey(), weightValue));
            }
        }
        if (adapted.isEmpty()) {
            throw new IllegalArgumentException("Weight map must contain at least one positive weight");
        }
        return weightedChoice(adapted.values());
    }

    private static double requireWeight(Number value) {
        Objects.requireNonNull(value, "weight");
        double numeric = value.doubleValue();
        if (Double.isNaN(numeric) || Double.isInfinite(numeric)) {
            throw new IllegalArgumentException("Weight must be finite");
        }
        if (numeric < 0d) {
            throw new IllegalArgumentException("Weight must not be negative");
        }
        return numeric;
    }
}
