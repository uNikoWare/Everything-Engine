package com.everythingengine.core.distribution;

import com.everythingengine.core.random.SeededRandom;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.spatial.Vector3D;

import java.util.Objects;

/**
 * Spherical radial sampling with probability density p(r) proportional to r^radialPower.
 * radialPower=2 corresponds to uniform volume density.
 */
public final class RadialDistribution implements PositionDistribution {
    private final double radiusParsecs;
    private final double radialPower;

    public RadialDistribution(double radiusParsecs, double radialPower) {
        if (!Double.isFinite(radiusParsecs) || radiusParsecs <= 0) {
            throw new IllegalArgumentException("radiusParsecs must be finite and positive");
        }
        if (!Double.isFinite(radialPower) || radialPower <= -1) {
            throw new IllegalArgumentException("radialPower must be finite and greater than -1");
        }
        this.radiusParsecs = radiusParsecs;
        this.radialPower = radialPower;
    }

    @Override
    public Position3D sample(SeededRandom random, Position3D center) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(center, "center");
        double radius = radiusParsecs * Math.pow(random.nextDouble(0, 1), 1 / (radialPower + 1));
        double zDirection = random.nextDouble(-1, 1);
        double angle = random.nextDouble(0, Math.PI * 2);
        double planar = Math.sqrt(Math.max(0, 1 - zDirection * zDirection));
        return center.translate(new Vector3D(
                radius * planar * Math.cos(angle),
                radius * planar * Math.sin(angle),
                radius * zDirection));
    }
}
