package com.everythingengine.core.distribution;

import com.everythingengine.core.galaxy.structure.SpiralArmGeometry;
import com.everythingengine.core.random.SeededRandom;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.spatial.Vector3D;

import java.util.Objects;

/**
 * Samples uniformly in spiral-arm parameter angle with bounded radial and vertical jitter.
 */
public final class SpiralDistribution implements PositionDistribution {
    private final SpiralArmGeometry arm;

    public SpiralDistribution(SpiralArmGeometry arm) {
        this.arm = Objects.requireNonNull(arm, "arm");
    }

    @Override
    public Position3D sample(SeededRandom random, Position3D center) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(center, "center");
        Position3D centerline = arm.centerlineAt(center, random.nextDouble(0, 1));
        double radialOffset = arm.widthParsecs() == 0 ? 0
                : random.nextDouble(-arm.widthParsecs() / 2, arm.widthParsecs() / 2);
        double angle = Math.atan2(centerline.y() - center.y(), centerline.x() - center.x());
        double zOffset = arm.verticalScaleParsecs() == 0 ? 0
                : random.nextDouble(-arm.verticalScaleParsecs() / 2, arm.verticalScaleParsecs() / 2);
        return center.translate(new Vector3D(
                centerline.x() - center.x() + radialOffset * Math.cos(angle),
                centerline.y() - center.y() + radialOffset * Math.sin(angle),
                centerline.z() - center.z() + zOffset));
    }
}
