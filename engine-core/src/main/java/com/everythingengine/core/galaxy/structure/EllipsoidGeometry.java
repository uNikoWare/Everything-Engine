package com.everythingengine.core.galaxy.structure;

import com.everythingengine.core.galaxy.GalaxyExtent;

import java.util.Objects;

/**
 * Axis-aligned ellipsoid geometry with semi-axis radii in parsecs.
 */
public record EllipsoidGeometry(GalaxyExtent extent) implements StructureGeometry {
    public EllipsoidGeometry {
        Objects.requireNonNull(extent, "extent");
    }
}
