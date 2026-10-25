package com.everythingengine.core.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;

public final class NameGenerator {
    private final com.everythingengine.core.name.NameGenerator delegate;

    public NameGenerator() {
        this.delegate = new com.everythingengine.core.name.NameGenerator();
    }

    public NameGenerator(long seed) {
        this.delegate = new com.everythingengine.core.name.NameGenerator(seed);
    }

    public void registerProfile(NameProfile profile) {
        delegate.registerProfile(profile.delegate());
    }

    public Optional<NameProfile> findProfile(String id) {
        return delegate.findProfile(id).map(profile -> new NameProfile(
                profile.id(), profile.name(), profile.description(), NameGenerationStrategy.valueOf(profile.strategy().name()),
                profile.prefixes(), profile.syllables(), profile.suffixes(), profile.words(), profile.template(),
                profile.minimumLength(), profile.maximumLength(), profile.deduplicateNames(), profile.maxAttempts(),
                profile.caseSensitiveDuplicates()));
    }

    public String generate(String profileId) {
        return delegate.generate(profileId);
    }

    public String generate(NameProfile profile) {
        return delegate.generate(profile.delegate());
    }

    public List<String> generateMany(String profileId, int count) {
        return delegate.generateMany(profileId, count);
    }

    public List<String> generateMany(NameProfile profile, int count) {
        return delegate.generateMany(profile.delegate(), count);
    }

    public List<String> generateMany(String profileId, int count, boolean avoidDuplicates) {
        return delegate.generateMany(profileId, count, avoidDuplicates);
    }

    public List<String> generateMany(NameProfile profile, int count, boolean avoidDuplicates) {
        return delegate.generateMany(profile.delegate(), count, avoidDuplicates);
    }

    public com.everythingengine.core.validation.ValidationResult validateProfile(NameProfile profile) {
        return delegate.validateProfile(profile.delegate());
    }
}
