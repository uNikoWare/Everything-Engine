package com.everythingengine.core.relation;

import com.everythingengine.core.entity.Entity;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable, directed relationship between two entities.
 */
public final class Relation {
    private final UUID id;
    private final Entity source;
    private final Entity target;
    private final RelationType type;

    public Relation(Entity source, Entity target, RelationType type) {
        this(UUID.randomUUID(), source, target, type);
    }

    public Relation(UUID id, Entity source, Entity target, RelationType type) {
        this.id = Objects.requireNonNull(id, "id");
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        this.type = Objects.requireNonNull(type, "type");
    }

    public UUID id() {
        return id;
    }

    public Entity source() {
        return source;
    }

    public Entity target() {
        return target;
    }

    public RelationType type() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Relation relation && id.equals(relation.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
