package com.everythingengine.core.spatial;

import java.util.Objects;

/**
 * Axis-aligned query bounds centered on a position. Half extents are in parsecs.
 */
public record SpatialBounds(Position3D center, Vector3D halfExtents) {
    public SpatialBounds {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(halfExtents, "halfExtents");
        if (halfExtents.x() < 0 || halfExtents.y() < 0 || halfExtents.z() < 0) {
            throw new IllegalArgumentException("Spatial bound half extents must not be negative");
        }
    }

    public boolean contains(Position3D position) {
        Objects.requireNonNull(position, "position");
        if (!center.space().equals(position.space())) {
            throw new IllegalArgumentException("Query and indexed positions must use the same coordinate space");
        }
        return Math.abs(position.x() - center.x()) <= halfExtents.x()
                && Math.abs(position.y() - center.y()) <= halfExtents.y()
                && Math.abs(position.z() - center.z()) <= halfExtents.z();
    }
}
