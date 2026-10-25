package com.everythingengine.core.attribute;

import java.util.Objects;

/**
 * Read-only description of an attribute for inspection tools.
 */
public record AttributeSnapshot(String name, Class<?> type, Object value) {
    public AttributeSnapshot {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        type.cast(value);
    }
}
