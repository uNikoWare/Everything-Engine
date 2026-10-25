package com.everythingengine.core.stellar;

import java.util.Objects;

/**
 * Extensible identifier for a stellar object kind.
 */
public record StellarObjectType(String key) {
    public static final StellarObjectType STAR = new StellarObjectType("star");
    public static final StellarObjectType BROWN_DWARF = new StellarObjectType("brown-dwarf");
    public static final StellarObjectType WHITE_DWARF = new StellarObjectType("white-dwarf");
    public static final StellarObjectType NEUTRON_STAR = new StellarObjectType("neutron-star");
    public static final StellarObjectType BLACK_HOLE = new StellarObjectType("black-hole");

    public StellarObjectType {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Stellar object type key must not be blank");
        }
    }
}
