package com.everythingengine.core.name;

import com.everythingengine.core.random.SeededRandom;
import com.everythingengine.core.validation.ValidationIssue;
import com.everythingengine.core.validation.ValidationResult;
import com.everythingengine.core.validation.ValidationRules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Generates names according to predefined name profiles.
 */
public final class NameGenerator {
    private final Map<String, NameProfile> profiles = new LinkedHashMap<>();
    private final SeededRandom random;

    public NameGenerator() {
        this(new SeededRandom());
    }

    public NameGenerator(long seed) {
        this(new SeededRandom(seed));
    }

    public NameGenerator(SeededRandom random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public void registerProfile(NameProfile profile) {
        Objects.requireNonNull(profile, "profile");
        profiles.put(profile.id(), profile);
    }

    public Optional<NameProfile> findProfile(String id) {
        return Optional.ofNullable(profiles.get(id));
    }

    public List<NameProfile> profiles() {
        return List.copyOf(profiles.values());
    }

    public ValidationResult validateProfile(NameProfile profile) {
        Objects.requireNonNull(profile, "profile");
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        if (profile.id() == null || profile.id().isBlank()) {
            errors.add(new ValidationIssue("id", "Profile id must not be blank", "profile.id"));
        }
        if (profile.name() == null || profile.name().isBlank()) {
            errors.add(new ValidationIssue("name", "Profile name must not be blank", "profile.name"));
        }
        if (profile.strategy() == null) {
            errors.add(new ValidationIssue("strategy", "Name generation strategy must be set", "profile.strategy"));
        }
        if (profile.minimumLength() != null && profile.maximumLength() != null
                && profile.minimumLength() > profile.maximumLength()) {
            errors.add(new ValidationIssue("length", "Minimum length may not exceed maximum length", "length.range"));
        }
        if (profile.maxAttempts() <= 0) {
            errors.add(new ValidationIssue("maxAttempts", "Maximum attempts must be positive", "profile.maxAttempts"));
        }

        switch (profile.strategy()) {
            case SYLLABLE -> {
                if (profile.syllables().isEmpty()) {
                    errors.add(new ValidationIssue("syllables", "Syllable strategy requires a non-empty syllable list",
                            "syllable.required"));
                }
            }
            case WORD -> {
                if (profile.words().isEmpty()) {
                    errors.add(new ValidationIssue("words", "Word strategy requires a non-empty word list",
                            "word.required"));
                }
            }
            case TEMPLATE -> {
                if (profile.template() == null || profile.template().isBlank()) {
                    errors.add(new ValidationIssue("template", "Template strategy requires a template string",
                            "template.required"));
                }
            }
            default -> throw new IllegalStateException("Unsupported strategy: " + profile.strategy());
        }

        if (profile.minimumLength() != null && profile.minimumLength() < 1) {
            warnings.add(new ValidationIssue("minimumLength", "Very short name lengths may produce noisy output",
                    "length.warning"));
        }

        return new ValidationResult(errors, warnings);
    }

    public String generate(String profileId) {
        NameProfile profile = findProfile(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown profile: " + profileId));
        return generate(profile);
    }

    public String generate(NameProfile profile) {
        ValidationResult validation = validateProfile(profile);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.errors().getFirst().message());
        }
        return generateInternal(profile);
    }

    public List<String> generateMany(String profileId, int count) {
        return generateMany(profileId, count, false);
    }

    public List<String> generateMany(NameProfile profile, int count) {
        return generateMany(profile, count, false);
    }

    public List<String> generateMany(String profileId, int count, boolean avoidDuplicates) {
        NameProfile profile = findProfile(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown profile: " + profileId));
        return generateMany(profile, count, avoidDuplicates);
    }

    public List<String> generateMany(NameProfile profile, int count, boolean avoidDuplicates) {
        ValidationResult validation = validateProfile(profile);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.errors().getFirst().message());
        }
        if (count < 0) {
            throw new IllegalArgumentException("Name count must not be negative");
        }
        if (count == 0) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String> generated = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = Math.max(profile.maxAttempts(), 1) * Math.max(count, 1) * 10;

        while (generated.size() < count && attempts < maxAttempts) {
            String candidate = generateInternal(profile);
            String normalized = profile.caseSensitiveDuplicates() ? candidate : candidate.toLowerCase(Locale.ROOT);
            if (avoidDuplicates && !seen.add(normalized)) {
                attempts++;
                continue;
            }
            if (profile.minimumLength() != null && candidate.length() < profile.minimumLength()) {
                attempts++;
                continue;
            }
            if (profile.maximumLength() != null && candidate.length() > profile.maximumLength()) {
                attempts++;
                continue;
            }
            generated.add(candidate);
            seen.add(normalized);
            attempts++;
        }

        if (generated.size() < count) {
            throw new IllegalStateException(
                    "Unable to generate enough unique names for profile '" + profile.id() + "' within the configured attempts");
        }
        return List.copyOf(generated);
    }

    public SeededRandom random() {
        return random;
    }

    private String generateInternal(NameProfile profile) {
        return switch (profile.strategy()) {
            case SYLLABLE -> generateSyllableName(profile);
            case WORD -> generateWordName(profile);
            case TEMPLATE -> generateTemplateName(profile);
        };
    }

    private String generateSyllableName(NameProfile profile) {
        String prefix = choose(profile.prefixes());
        String syllable = choose(profile.syllables());
        String suffix = choose(profile.suffixes());

        StringBuilder builder = new StringBuilder();
        if (!profile.prefixes().isEmpty()) {
            builder.append(prefix);
        }
        builder.append(syllable);
        if (!profile.suffixes().isEmpty()) {
            builder.append(suffix);
        }
        return builder.toString();
    }

    private String generateWordName(NameProfile profile) {
        if (profile.words().isEmpty()) {
            throw new IllegalArgumentException("Word names require a non-empty word list");
        }

        StringBuilder builder = new StringBuilder();
        int partCount = random.nextInt(2, 4);
        for (int index = 0; index < partCount; index++) {
            String word = choose(profile.words());
            builder.append(word);
        }
        return builder.toString();
    }

    private String generateTemplateName(NameProfile profile) {
        String template = profile.template();
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < template.length(); index++) {
            char current = template.charAt(index);
            if (current == '{') {
                int close = template.indexOf('}', index);
                if (close < 0) {
                    throw new IllegalArgumentException("Invalid template syntax in profile '" + profile.id() + "'");
                }
                String token = template.substring(index + 1, close);
                String replacement = resolvePlaceholder(token, profile);
                result.append(replacement);
                index = close;
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    private String resolvePlaceholder(String token, NameProfile profile) {
        String normalized = token.trim();
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "NUMBER" -> Integer.toString(random.nextInt(0, 9999));
            case "CODE" -> randomAlphaNumericCode();
            case "PREFIX" -> choose(profile.prefixes());
            case "SUFFIX" -> choose(profile.suffixes());
            case "WORD" -> choose(profile.words());
            case "SYLLABLE" -> choose(profile.syllables());
            default -> throw new IllegalArgumentException("Unknown template placeholder: " + token);
        };
    }

    private String randomAlphaNumericCode() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int index = 0; index < 4; index++) {
            code.append(letters.charAt(random.nextInt(0, letters.length() - 1)));
        }
        return code.toString();
    }

    private <T> T choose(List<T> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("List must not be empty");
        }
        return random.choose(values);
    }
}
