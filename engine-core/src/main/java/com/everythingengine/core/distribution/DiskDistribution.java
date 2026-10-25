package com.everythingengine.core.distribution;

import com.everythingengine.core.random.SeededRandom;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.spatial.Vector3D;

import java.util.Objects;

/**
 * Uniform area density in a circular disk, with uniform vertical thickness.
 */
public final class DiskDistribution implements PositionDistribution {
    private final double radiusParsecs;
    private final double thicknessParsecs;

    public DiskDistribution(double radiusParsecs, double thicknessParsecs) {
        requireFinite(radiusParsecs, "radiusParsecs");
        requireFinite(thicknessParsecs, "thicknessParsecs");
        if (radiusParsecs <= 0 || thicknessParsecs < 0) {
            throw new IllegalArgumentException("Disk radius must be positive and thickness non-negative");
        }
        this.radiusParsecs = radiusParsecs;
        this.thicknessParsecs = thicknessParsecs;
    }

    @Override
    public Position3D sample(SeededRandom random, Position3D center) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(center, "center");
        double radius = radiusParsecs * Math.sqrt(random.nextDouble(0, 1));
        double angle = random.nextDouble(0, Math.PI * 2);
        double z = thicknessParsecs == 0 ? 0
                : random.nextDouble(-thicknessParsecs / 2, thicknessParsecs / 2);
        return center.translate(new Vector3D(radius * Math.cos(angle), radius * Math.sin(angle), z));
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
