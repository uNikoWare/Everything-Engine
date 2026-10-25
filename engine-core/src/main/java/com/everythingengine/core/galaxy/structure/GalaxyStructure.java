package com.everythingengine.core.galaxy.structure;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.spatial.Position3D;

import java.util.Objects;
import java.util.UUID;

/**
 * Spatial component belonging to a galaxy. Relationships are registered separately in Universe.
 */
public final class GalaxyStructure extends Entity {
    private final Galaxy galaxy;
    private final GalacticStructureType type;
    private final Position3D position;
    private final StructureGeometry geometry;

    public GalaxyStructure(String name, Galaxy galaxy, GalacticStructureType type,
            Position3D position, StructureGeometry geometry) {
        this(UUID.randomUUID(), name, galaxy, type, position, geometry);
    }

    public GalaxyStructure(UUID id, String name, Galaxy galaxy, GalacticStructureType type,
            Position3D position, StructureGeometry geometry) {
        super(id, name);
        this.galaxy = Objects.requireNonNull(galaxy, "galaxy");
        this.type = Objects.requireNonNull(type, "type");
        this.position = Objects.requireNonNull(position, "position");
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        if (!galaxy.position().space().equals(position.space())) {
            throw new IllegalArgumentException("Structure and galaxy positions must use the same coordinate space");
        }
    }

    public Galaxy galaxy() {
        return galaxy;
    }

    public GalacticStructureType type() {
        return type;
    }

    public Position3D position() {
        return position;
    }

    public StructureGeometry geometry() {
        return geometry;
    }
}
