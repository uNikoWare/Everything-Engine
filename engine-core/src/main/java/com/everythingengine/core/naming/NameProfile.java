package com.everythingengine.core.naming;

import java.util.List;

public record NameProfile(
        String id,
        String name,
        String description,
        NameGenerationStrategy strategy,
        List<String> prefixes,
        List<String> syllables,
        List<String> suffixes,
        List<String> words,
        String template,
        Integer minimumLength,
        Integer maximumLength,
        boolean deduplicateNames,
        int maxAttempts,
        boolean caseSensitiveDuplicates) {

    public NameProfile(String id, String name, String description, NameGenerationStrategy strategy) {
        this(id, name, description, strategy, List.of(), List.of(), List.of(), List.of(), null, null, null,
                false, 25, false);
    }

    public NameProfile(String id, String name, String description, String strategy) {
        this(id, name, description, NameGenerationStrategy.parse(strategy));
    }

    public com.everythingengine.core.name.NameProfile delegate() {
        return new com.everythingengine.core.name.NameProfile(
                id, name, description, com.everythingengine.core.name.NameGenerationStrategy.valueOf(strategy.name()),
                prefixes, syllables, suffixes, words, template, minimumLength, maximumLength,
                deduplicateNames, maxAttempts, caseSensitiveDuplicates);
    }
}
