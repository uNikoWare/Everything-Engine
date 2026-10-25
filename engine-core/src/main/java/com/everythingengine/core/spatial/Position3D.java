package com.everythingengine.core.spatial;

import java.util.Objects;

/**
 * A position in a named coordinate frame, measured in parsecs.
 *
 * <p>The axes form a right-handed Cartesian system: +X cross +Y points along +Z.</p>
 */
public record Position3D(CoordinateSpace space, double x, double y, double z) {
    public Position3D {
        Objects.requireNonNull(space, "space");
        Vector3D.requireFinite(x, "x");
        Vector3D.requireFinite(y, "y");
        Vector3D.requireFinite(z, "z");
    }

    public double distanceTo(Position3D other) {
        requireSameSpace(other);
        return Math.hypot(Math.hypot(x - other.x, y - other.y), z - other.z);
    }

    public Direction3D directionTo(Position3D other) {
        requireSameSpace(other);
        return Direction3D.from(new Vector3D(other.x - x, other.y - y, other.z - z));
    }

    public Position3D translate(Vector3D displacement) {
        Objects.requireNonNull(displacement, "displacement");
        return new Position3D(space, x + displacement.x(), y + displacement.y(), z + displacement.z());
    }

    private void requireSameSpace(Position3D other) {
        Objects.requireNonNull(other, "other");
        if (!space.equals(other.space)) {
            throw new IllegalArgumentException("Positions must use the same coordinate space");
        }
    }
}
