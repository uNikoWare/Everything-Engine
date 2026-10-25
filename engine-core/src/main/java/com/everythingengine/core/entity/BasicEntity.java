package com.everythingengine.core.entity;

import java.util.UUID;

/**
 * General-purpose entity for tools that do not need a domain-specific subtype.
 */
public final class BasicEntity extends Entity {
    public BasicEntity(String name) {
        super(name);
    }

    public BasicEntity(UUID id, String name) {
        super(id, name);
    }
}
