package com.everythingengine.core.galaxy;

import com.everythingengine.core.spatial.Vector3D;

/**
 * Semi-axis radii of a bounding ellipsoid, in parsecs.
 */
public record GalaxyExtent(double radiusXParsecs, double radiusYParsecs, double radiusZParsecs) {
    public GalaxyExtent {
        requirePositive(radiusXParsecs, "radiusXParsecs");
        requirePositive(radiusYParsecs, "radiusYParsecs");
        requirePositive(radiusZParsecs, "radiusZParsecs");
    }

    public static GalaxyExtent spherical(double radiusParsecs) {
        return new GalaxyExtent(radiusParsecs, radiusParsecs, radiusParsecs);
    }

    public boolean containsOffset(Vector3D offset) {
        double x = offset.x() / radiusXParsecs;
        double y = offset.y() / radiusYParsecs;
        double z = offset.z() / radiusZParsecs;
        return x * x + y * y + z * z <= 1.0;
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be finite and greater than zero");
        }
    }
}
