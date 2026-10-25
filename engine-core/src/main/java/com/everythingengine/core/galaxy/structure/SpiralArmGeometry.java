package com.everythingengine.core.galaxy.structure;

import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.spatial.Position3D;

import java.util.Objects;

/**
 * Logarithmic spiral centered on a galaxy position.
 * Angle is in radians; radii and vertical scale are in parsecs.
 */
public record SpiralArmGeometry(
        double innerRadiusParsecs,
        double pitchAngleRadians,
        double startAngleRadians,
        double turns,
        double verticalScaleParsecs,
        double widthParsecs) implements StructureGeometry {

    public SpiralArmGeometry {
        requireFinite(innerRadiusParsecs, "innerRadiusParsecs");
        requireFinite(pitchAngleRadians, "pitchAngleRadians");
        requireFinite(startAngleRadians, "startAngleRadians");
        requireFinite(turns, "turns");
        requireFinite(verticalScaleParsecs, "verticalScaleParsecs");
        requireFinite(widthParsecs, "widthParsecs");
        if (innerRadiusParsecs <= 0 || pitchAngleRadians <= 0 || pitchAngleRadians >= Math.PI / 2
                || turns <= 0 || verticalScaleParsecs < 0 || widthParsecs < 0) {
            throw new IllegalArgumentException("Spiral parameters must define positive radii, turns and pitch");
        }
    }

    public Position3D centerlineAt(Galaxy galaxy, double fraction) {
        Objects.requireNonNull(galaxy, "galaxy");
        return centerlineAt(galaxy.position(), fraction);
    }

    public Position3D centerlineAt(Position3D center, double fraction) {
        Objects.requireNonNull(center, "center");
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("fraction must be finite and in [0, 1]");
        }
        double angle = startAngleRadians + fraction * turns * Math.PI * 2;
        double radius = innerRadiusParsecs * Math.exp(Math.tan(pitchAngleRadians) * (angle - startAngleRadians));
        return new Position3D(center.space(),
                center.x() + radius * Math.cos(angle),
                center.y() + radius * Math.sin(angle),
                center.z());
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
