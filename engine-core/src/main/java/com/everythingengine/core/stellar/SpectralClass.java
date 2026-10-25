package com.everythingengine.core.stellar;

import java.util.Objects;

/**
 * Extensible stellar spectral classification label.
 */
public record SpectralClass(String key) {
    public static final SpectralClass O = new SpectralClass("O");
    public static final SpectralClass B = new SpectralClass("B");
    public static final SpectralClass A = new SpectralClass("A");
    public static final SpectralClass F = new SpectralClass("F");
    public static final SpectralClass G = new SpectralClass("G");
    public static final SpectralClass K = new SpectralClass("K");
    public static final SpectralClass M = new SpectralClass("M");

    public SpectralClass {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Spectral class key must not be blank");
        }
    }
}
