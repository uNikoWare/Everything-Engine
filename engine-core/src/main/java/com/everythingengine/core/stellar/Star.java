package com.everythingengine.core.stellar;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Star properties use solar radii/luminosities and kelvin.
 */
public final class Star extends StellarObject {
    private final OptionalDouble radiusSolar;
    private final OptionalDouble luminositySolar;
    private final OptionalDouble effectiveTemperatureKelvin;
    private final Optional<SpectralClass> spectralClass;
    private final Optional<StellarEvolutionState> evolutionState;

    public Star(String name, OptionalDouble massSolar, OptionalDouble radiusSolar,
            OptionalDouble luminositySolar, OptionalDouble effectiveTemperatureKelvin,
            Optional<SpectralClass> spectralClass, Optional<StellarEvolutionState> evolutionState,
            OptionalDouble ageYears, OptionalDouble metallicityFeH) {
        this(UUID.randomUUID(), name, massSolar, radiusSolar, luminositySolar, effectiveTemperatureKelvin,
                spectralClass, evolutionState, ageYears, metallicityFeH);
    }

    public Star(UUID id, String name, OptionalDouble massSolar, OptionalDouble radiusSolar,
            OptionalDouble luminositySolar, OptionalDouble effectiveTemperatureKelvin,
            Optional<SpectralClass> spectralClass, Optional<StellarEvolutionState> evolutionState,
            OptionalDouble ageYears, OptionalDouble metallicityFeH) {
        super(id, name, StellarObjectType.STAR, massSolar, ageYears, metallicityFeH);
        this.radiusSolar = positive(radiusSolar, "radiusSolar");
        this.luminositySolar = positive(luminositySolar, "luminositySolar");
        this.effectiveTemperatureKelvin = positive(effectiveTemperatureKelvin, "effectiveTemperatureKelvin");
        this.spectralClass = Objects.requireNonNull(spectralClass, "spectralClass");
        this.evolutionState = Objects.requireNonNull(evolutionState, "evolutionState");
    }

    public OptionalDouble radiusSolar() {
        return radiusSolar;
    }

    public OptionalDouble luminositySolar() {
        return luminositySolar;
    }

    public OptionalDouble effectiveTemperatureKelvin() {
        return effectiveTemperatureKelvin;
    }

    public Optional<SpectralClass> spectralClass() {
        return spectralClass;
    }

    public Optional<StellarEvolutionState> evolutionState() {
        return evolutionState;
    }

    private static OptionalDouble positive(OptionalDouble value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isPresent() && (!Double.isFinite(value.getAsDouble()) || value.getAsDouble() <= 0)) {
            throw new IllegalArgumentException(name + " must be finite and positive when present");
        }
        return value;
    }
}
