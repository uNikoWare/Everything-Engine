package com.everythingengine.core.stellar;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.galaxy.structure.GalaxyStructure;
import com.everythingengine.core.spatial.Position3D;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Spatially located system belonging to a galaxy. Star membership is represented by Universe relations.
 */
public final class StellarSystem extends Entity {
    private final Galaxy galaxy;
    private final Position3D position;
    private final Optional<GalaxyStructure> region;

    public StellarSystem(String name, Galaxy galaxy, Position3D position, Optional<GalaxyStructure> region) {
        this(UUID.randomUUID(), name, galaxy, position, region);
    }

    public StellarSystem(UUID id, String name, Galaxy galaxy, Position3D position, Optional<GalaxyStructure> region) {
        super(id, name);
        this.galaxy = Objects.requireNonNull(galaxy, "galaxy");
        this.position = Objects.requireNonNull(position, "position");
        this.region = Objects.requireNonNull(region, "region");
        if (!galaxy.position().space().equals(position.space())) {
            throw new IllegalArgumentException("System and galaxy positions must use the same coordinate space");
        }
        if (region.isPresent() && region.get().galaxy() != galaxy) {
            throw new IllegalArgumentException("System region must belong to its galaxy");
        }
    }

    public Galaxy galaxy() {
        return galaxy;
    }

    public Position3D position() {
        return position;
    }

    public Optional<GalaxyStructure> region() {
        return region;
    }
}
