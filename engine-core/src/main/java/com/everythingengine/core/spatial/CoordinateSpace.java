package com.everythingengine.core.spatial;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies a coordinate frame. Spatial coordinates are measured in parsecs.
 */
public record CoordinateSpace(UUID id, String name) {
    public CoordinateSpace {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Coordinate space name must not be blank");
        }
    }

    public CoordinateSpace(String name) {
        this(UUID.randomUUID(), name);
    }
}
