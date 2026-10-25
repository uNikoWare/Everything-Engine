package com.everythingengine.core.naming;

public enum NameGenerationStrategy {
    SYLLABLE,
    WORD,
    TEMPLATE;

    public static NameGenerationStrategy parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Strategy must not be blank");
        }
        return NameGenerationStrategy.valueOf(value.trim().toUpperCase());
    }
}
