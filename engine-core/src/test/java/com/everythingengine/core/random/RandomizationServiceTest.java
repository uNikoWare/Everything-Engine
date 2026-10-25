package com.everythingengine.core.random;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RandomizationServiceTest {
    @Test
    void sameSeedProducesRepeatableSequence() {
        SeededRandom first = new SeededRandom(42L);
        SeededRandom second = new SeededRandom(42L);

        assertEquals(first.nextInt(0, 10), second.nextInt(0, 10));
        assertEquals(first.nextDouble(0d, 1d), second.nextDouble(0d, 1d), 1e-12);
        assertEquals(first.nextBoolean(50d), second.nextBoolean(50d));
    }

    @Test
    void probabilityRangeAndWeightedSelectionAreValidated() {
        SeededRandom random = new SeededRandom(7L);

        assertThrows(IllegalArgumentException.class, () -> random.nextBoolean(-1d));
        assertThrows(IllegalArgumentException.class, () -> random.nextBoolean(101d));
        assertThrows(IllegalArgumentException.class, () -> random.weightedChoice(List.of()));

        String picked = random.weightedChoice(List.of(
                new WeightedChoice<>("rock", 60d),
                new WeightedChoice<>("gas", 25d),
                new WeightedChoice<>("ice", 15d)));
        assertTrue(List.of("rock", "gas", "ice").contains(picked));
    }

    @Test
    void independentInstancesDoNotShareSequentialState() {
        SeededRandom first = new SeededRandom(9L);
        SeededRandom second = new SeededRandom(10L);

        int firstValue = first.nextInt(0, 100);
        int secondValue = second.nextInt(0, 100);

        assertNotEquals(firstValue, secondValue);
    }
}
