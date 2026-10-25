package com.everythingengine.core.randomization;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Compatibility wrapper for the main randomization support API.
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

    public static double randomDouble(double minInclusive, double maxExclusive) {
        return create().nextDouble(minInclusive, maxExclusive);
    }

    public static boolean randomBoolean() {
        return create().nextBoolean();
    }

    public static boolean randomBoolean(double probabilityPercent) {
        return create().nextBoolean(probabilityPercent);
    }

    public static <T> T choose(Collection<T> values) {
        return create().choose(values);
    }

    public static <T> T weightedChoice(Collection<WeightedChoice<T>> values) {
        return create().weightedChoice(values);
    }

    public static <T> T weightedChoice(Map<T, Number> weights) {
        return create().weightedChoice(weights);
    }
}
