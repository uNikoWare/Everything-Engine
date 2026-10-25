package com.everythingengine.core.galaxy.structure;

import java.util.Objects;

/**
 * Extensible identifier for a galaxy structure kind.
 */
public record GalacticStructureType(String key) {
    public static final GalacticStructureType CORE = new GalacticStructureType("core");
    public static final GalacticStructureType BULGE = new GalacticStructureType("bulge");
    public static final GalacticStructureType DISK = new GalacticStructureType("disk");
    public static final GalacticStructureType SPIRAL_ARM = new GalacticStructureType("spiral-arm");
    public static final GalacticStructureType BAR = new GalacticStructureType("bar");
    public static final GalacticStructureType HALO = new GalacticStructureType("halo");
    public static final GalacticStructureType REGION = new GalacticStructureType("region");

    public GalacticStructureType {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Structure type key must not be blank");
        }
    }
}
