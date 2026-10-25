package com.everythingengine.core.relation;

import java.util.Objects;

/**
 * Extensible stable identifier for a relation kind.
 */
public record RelationType(String key) {
    public RelationType {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Relation type key must not be blank");
        }
    }
}
