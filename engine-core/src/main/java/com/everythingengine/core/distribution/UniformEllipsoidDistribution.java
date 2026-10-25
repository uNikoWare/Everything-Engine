package com.everythingengine.core.distribution;

import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.random.SeededRandom;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.spatial.Vector3D;

import java.util.Objects;

/**
 * Uniform volume density in an axis-aligned ellipsoid centered on the origin.
 */
public final class UniformEllipsoidDistribution implements PositionDistribution {
    private final GalaxyExtent extent;

    public UniformEllipsoidDistribution(GalaxyExtent extent) {
        this.extent = Objects.requireNonNull(extent, "extent");
    }

    @Override
    public Position3D sample(SeededRandom random, Position3D center) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(center, "center");
        for (int attempt = 0; attempt < 10_000; attempt++) {
            double x = random.nextDouble(-1, 1);
            double y = random.nextDouble(-1, 1);
            double z = random.nextDouble(-1, 1);
            if (x * x + y * y + z * z <= 1) {
                return center.translate(new Vector3D(
                        x * extent.radiusXParsecs(),
                        y * extent.radiusYParsecs(),
                        z * extent.radiusZParsecs()));
            }
        }
        throw new IllegalStateException("Could not sample a position inside the ellipsoid");
    }
}
