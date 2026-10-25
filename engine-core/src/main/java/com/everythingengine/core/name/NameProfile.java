package com.everythingengine.core.name;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes how a generated name should be assembled.
 */
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

    public NameProfile {
        id = requireText(id, "id");
        name = requireText(name, "name");
        description = description == null ? "" : description;
        strategy = Objects.requireNonNull(strategy, "strategy");
        prefixes = normalizeList(prefixes);
        syllables = normalizeList(syllables);
        suffixes = normalizeList(suffixes);
        words = normalizeList(words);
        template = template == null ? "" : template.trim();
        if (minimumLength != null && minimumLength < 0) {
            throw new IllegalArgumentException("Minimum length must not be negative");
        }
        if (maximumLength != null && maximumLength < 0) {
            throw new IllegalArgumentException("Maximum length must not be negative");
        }
        if (minimumLength != null && maximumLength != null && minimumLength > maximumLength) {
            throw new IllegalArgumentException("Minimum length must not exceed maximum length");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Maximum attempts must be positive");
        }
    }

    public NameProfile(String id, String name, String description, NameGenerationStrategy strategy) {
        this(id, name, description, strategy, List.of(), List.of(), List.of(), List.of(), null, null, null,
                false, 25, false);
    }

    public NameProfile(String id, String name, String description, String strategy) {
        this(id, name, description, NameGenerationStrategy.parse(strategy));
    }

    public NameProfile(
            String id,
            String name,
            String description,
            NameGenerationStrategy strategy,
            List<String> prefixes,
            List<String> syllables,
            List<String> suffixes) {
        this(id, name, description, strategy, prefixes, syllables, suffixes, List.of(), null, null, null, false,
                25, false);
    }

    public NameProfile(
            String id,
            String name,
            String description,
            NameGenerationStrategy strategy,
            List<String> prefixes,
            List<String> syllables,
            List<String> suffixes,
            List<String> words) {
        this(id, name, description, strategy, prefixes, syllables, suffixes, words, null, null, null, false,
                25, false);
    }

    public NameProfile(
            String id,
            String name,
            String description,
            NameGenerationStrategy strategy,
            List<String> prefixes,
            List<String> syllables,
            List<String> suffixes,
            List<String> words,
            String template) {
        this(id, name, description, strategy, prefixes, syllables, suffixes, words, template, null, null, false,
                25, false);
    }

    public boolean hasSyllableData() {
        return !syllables.isEmpty();
    }

    public boolean hasWords() {
        return !words.isEmpty();
    }

    public boolean hasTemplate() {
        return !template.isBlank();
    }

    public String normalizeCase(String value) {
        return caseSensitiveDuplicates ? value : value.toLowerCase();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                normalized.add(text);
            }
        }
        return List.copyOf(normalized);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }
}
