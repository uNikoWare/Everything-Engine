package com.everythingengine.core.entity;

import com.everythingengine.core.attribute.AttributeKey;
import com.everythingengine.core.attribute.AttributeSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Base class for extensible universe objects.
 */
public abstract class Entity {
    private final UUID id;
    private String name;
    private final Map<String, AttributeValue<?>> attributes = new HashMap<>();

    protected Entity(String name) {
        this(UUID.randomUUID(), name);
    }

    protected Entity(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        setName(name);
    }

    public final UUID id() {
        return id;
    }

    public final String name() {
        return name;
    }

    public final void setName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Entity name must not be blank");
        }
        this.name = name;
    }

    public final <T> void setAttribute(AttributeKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        key.type().cast(value);

        AttributeValue<?> existing = attributes.get(key.name());
        if (existing != null && !existing.key().type().equals(key.type())) {
            throw new IllegalArgumentException(
                    "Attribute '" + key.name() + "' is already defined as " + existing.key().type().getName());
        }
        attributes.put(key.name(), new AttributeValue<>(key, value));
    }

    public final <T> Optional<T> getAttribute(AttributeKey<T> key) {
        Objects.requireNonNull(key, "key");
        AttributeValue<?> stored = attributes.get(key.name());
        if (stored == null) {
            return Optional.empty();
        }
        if (!stored.key().type().equals(key.type())) {
            throw new IllegalArgumentException(
                    "Attribute '" + key.name() + "' is stored as " + stored.key().type().getName());
        }
        return Optional.of(key.type().cast(stored.value()));
    }

    public final boolean hasAttribute(AttributeKey<?> key) {
        Objects.requireNonNull(key, "key");
        AttributeValue<?> stored = attributes.get(key.name());
        if (stored == null) {
            return false;
        }
        if (!stored.key().type().equals(key.type())) {
            throw new IllegalArgumentException(
                    "Attribute '" + key.name() + "' is stored as " + stored.key().type().getName());
        }
        return true;
    }

    public final boolean removeAttribute(AttributeKey<?> key) {
        Objects.requireNonNull(key, "key");
        AttributeValue<?> stored = attributes.get(key.name());
        if (stored == null) {
            return false;
        }
        if (!stored.key().type().equals(key.type())) {
            throw new IllegalArgumentException(
                    "Attribute '" + key.name() + "' is stored as " + stored.key().type().getName());
        }
        attributes.remove(key.name());
        return true;
    }

    public final List<AttributeSnapshot> attributes() {
        return attributes.values().stream()
                .map(value -> new AttributeSnapshot(
                        value.key().name(), value.key().type(), value.value()))
                .toList();
    }

    @Override
    public final boolean equals(Object other) {
        return this == other || other instanceof Entity entity && id.equals(entity.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    private record AttributeValue<T>(AttributeKey<T> key, T value) {
    }
}
