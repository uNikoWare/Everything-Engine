package com.everythingengine.core.stellar;

import java.util.Objects;

public record StellarEvolutionState(String key) {
    public static final StellarEvolutionState MAIN_SEQUENCE = new StellarEvolutionState("main-sequence");
    public static final StellarEvolutionState PROTOSTAR = new StellarEvolutionState("protostar");
    public static final StellarEvolutionState GIANT = new StellarEvolutionState("giant");
    public static final StellarEvolutionState REMNANT = new StellarEvolutionState("remnant");

    public StellarEvolutionState {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Evolution state key must not be blank");
        }
    }
}
