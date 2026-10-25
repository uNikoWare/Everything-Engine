package com.everythingengine.core.name;

import java.util.Locale;
import java.util.Objects;

/**
 * Strategy used when generating a name from a profile.
 */
public enum NameGenerationStrategy {
    SYLLABLE,
    WORD,
    TEMPLATE;

    public static NameGenerationStrategy parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Strategy must not be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return NameGenerationStrategy.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported name generation strategy: " + value, exception);
        }
    }
}
