package com.everythingengine.core.galaxy;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.spatial.Position3D;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Galaxy domain object. Population estimates are independent of materialized systems.
 */
public final class Galaxy extends Entity {
    private final GalaxyType type;
    private final Position3D position;
    private final GalaxyExtent extent;
    private final OptionalDouble ageYears;
    private final OptionalDouble estimatedStellarMassKilograms;
    private final OptionalLong estimatedStarCount;

    public Galaxy(String name, GalaxyType type, Position3D position, GalaxyExtent extent) {
        this(name, type, position, extent, OptionalDouble.empty(), OptionalDouble.empty(), OptionalLong.empty());
    }

    public Galaxy(String name, GalaxyType type, Position3D position, GalaxyExtent extent,
            OptionalDouble ageYears, OptionalDouble estimatedStellarMassKilograms, OptionalLong estimatedStarCount) {
        this(UUID.randomUUID(), name, type, position, extent, ageYears, estimatedStellarMassKilograms,
                estimatedStarCount);
    }

    public Galaxy(UUID id, String name, GalaxyType type, Position3D position, GalaxyExtent extent,
            OptionalDouble ageYears, OptionalDouble estimatedStellarMassKilograms, OptionalLong estimatedStarCount) {
        super(id, name);
        this.type = Objects.requireNonNull(type, "type");
        this.position = Objects.requireNonNull(position, "position");
        this.extent = Objects.requireNonNull(extent, "extent");
        this.ageYears = validateNonNegative(ageYears, "ageYears");
        this.estimatedStellarMassKilograms =
                validateNonNegative(estimatedStellarMassKilograms, "estimatedStellarMassKilograms");
        this.estimatedStarCount = Objects.requireNonNull(estimatedStarCount, "estimatedStarCount");
        if (estimatedStarCount.isPresent() && estimatedStarCount.getAsLong() < 0) {
            throw new IllegalArgumentException("estimatedStarCount must not be negative");
        }
    }

    public GalaxyType type() {
        return type;
    }

    public Position3D position() {
        return position;
    }

    public GalaxyExtent extent() {
        return extent;
    }

    public OptionalDouble ageYears() {
        return ageYears;
    }

    public OptionalDouble estimatedStellarMassKilograms() {
        return estimatedStellarMassKilograms;
    }

    public OptionalLong estimatedStarCount() {
        return estimatedStarCount;
    }

    private static OptionalDouble validateNonNegative(OptionalDouble value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isPresent() && (!Double.isFinite(value.getAsDouble()) || value.getAsDouble() < 0)) {
            throw new IllegalArgumentException(name + " must be finite and non-negative when present");
        }
        return value;
    }
}
