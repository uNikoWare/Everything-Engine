package com.everythingengine.core.galaxy;

import java.util.Objects;

/**
 * Extensible identifier for a galaxy morphology.
 */
public record GalaxyType(String key) {
    public static final GalaxyType SPIRAL = new GalaxyType("spiral");
    public static final GalaxyType BARRED_SPIRAL = new GalaxyType("barred-spiral");
    public static final GalaxyType ELLIPTICAL = new GalaxyType("elliptical");
    public static final GalaxyType LENTICULAR = new GalaxyType("lenticular");
    public static final GalaxyType IRREGULAR = new GalaxyType("irregular");

    public GalaxyType {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Galaxy type key must not be blank");
        }
    }

    public static GalaxyType custom(String key) {
        return new GalaxyType(key);
    }
}
