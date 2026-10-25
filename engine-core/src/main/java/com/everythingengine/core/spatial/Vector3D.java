package com.everythingengine.core.spatial;

/**
 * A displacement in three-dimensional space, measured in parsecs.
 */
public record Vector3D(double x, double y, double z) {
    public Vector3D {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public Vector3D add(Vector3D other) {
        return new Vector3D(x + other.x, y + other.y, z + other.z);
    }

    public Vector3D subtract(Vector3D other) {
        return new Vector3D(x - other.x, y - other.y, z - other.z);
    }

    public Vector3D scale(double factor) {
        requireFinite(factor, "factor");
        return new Vector3D(x * factor, y * factor, z * factor);
    }

    public double magnitude() {
        return Math.hypot(Math.hypot(x, y), z);
    }

    public Vector3D normalized() {
        double magnitude = magnitude();
        if (magnitude == 0.0) {
            throw new IllegalStateException("A zero vector has no direction");
        }
        return scale(1.0 / magnitude);
    }

    static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
