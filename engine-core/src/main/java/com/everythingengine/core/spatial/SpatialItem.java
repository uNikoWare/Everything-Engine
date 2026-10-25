package com.everythingengine.core.spatial;

import java.util.Objects;
import java.util.UUID;

/**
 * A value's spatial index entry; the index is derived and does not own the value.
 */
public record SpatialItem<T>(UUID id, T value, Position3D position) {
    public SpatialItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(position, "position");
    }
}
