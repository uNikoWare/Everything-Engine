package com.everythingengine.core.attribute;

import java.util.Objects;

/**
 * Stable, typed identifier for an entity attribute.
 */
public final class AttributeKey<T> {
    private final String name;
    private final Class<T> type;

    public AttributeKey(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Attribute key must not be blank");
        }
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AttributeKey<?> key && name.equals(key.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
