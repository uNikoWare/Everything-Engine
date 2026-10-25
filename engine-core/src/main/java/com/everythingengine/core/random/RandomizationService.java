package com.everythingengine.core.random;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Convenience API for deterministic and reproducible random operations.
 */
public final class RandomizationService {
    private RandomizationService() {
    }

    public static SeededRandom create() {
        return SeededRandom.create();
    }

    public static SeededRandom create(long seed) {
        return SeededRandom.create(seed);
    }

    public static SeededRandom fromSeed(long seed) {
        return SeededRandom.fromSeed(seed);
    }

    public static int randomInt(int minInclusive, int maxInclusive) {
        return create().nextInt(minInclusive, maxInclusive);
    }

    public static int randomInt(SeededRandom random, int minInclusive, int maxInclusive) {
        Objects.requireNonNull(random, "random");
        return random.nextInt(minInclusive, maxInclusive);
    }

    public static double randomDouble(double minInclusive, double maxExclusive) {
        return create().nextDouble(minInclusive, maxExclusive);
    }

    public static double randomDouble(SeededRandom random, double minInclusive, double maxExclusive) {
        Objects.requireNonNull(random, "random");
        return random.nextDouble(minInclusive, maxExclusive);
    }

    public static boolean randomBoolean() {
        return create().nextBoolean();
    }

    public static boolean randomBoolean(SeededRandom random) {
        Objects.requireNonNull(random, "random");
        return random.nextBoolean();
    }

    public static boolean randomBoolean(double probabilityPercent) {
        return create().nextBoolean(probabilityPercent);
    }

    public static boolean randomBoolean(SeededRandom random, double probabilityPercent) {
        Objects.requireNonNull(random, "random");
        return random.nextBoolean(probabilityPercent);
    }

    public static <T> T choose(Collection<T> values) {
        return create().choose(values);
    }

    public static <T> T choose(SeededRandom random, Collection<T> values) {
        Objects.requireNonNull(random, "random");
        return random.choose(values);
    }

    public static <T> T weightedChoice(Collection<WeightedChoice<T>> options) {
        return create().weightedChoice(options);
    }

    public static <T> T weightedChoice(SeededRandom random, Collection<WeightedChoice<T>> options) {
        Objects.requireNonNull(random, "random");
        return random.weightedChoice(options);
    }

    public static <T> T weightedChoice(Map<T, Number> weights) {
        return create().weightedChoice(weights);
    }

    public static <T> T weightedChoice(SeededRandom random, Map<T, Number> weights) {
        Objects.requireNonNull(random, "random");
        return random.weightedChoice(weights);
    }
}
