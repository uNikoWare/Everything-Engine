package com.everythingengine.core.spatial;

/**
 * A unitless direction in a right-handed Cartesian frame.
 */
public record Direction3D(double x, double y, double z) {
    public Direction3D {
        Vector3D.requireFinite(x, "x");
        Vector3D.requireFinite(y, "y");
        Vector3D.requireFinite(z, "z");
        double length = Math.hypot(Math.hypot(x, y), z);
        if (Math.abs(length - 1.0) > 1.0e-10) {
            throw new IllegalArgumentException("Direction vector must have unit length");
        }
    }

    public static Direction3D from(Vector3D vector) {
        Vector3D unit = vector.normalized();
        return new Direction3D(unit.x(), unit.y(), unit.z());
    }

    public Vector3D asVector() {
        return new Vector3D(x, y, z);
    }
}
