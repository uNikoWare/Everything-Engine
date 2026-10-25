package com.everythingengine.core.name;

import java.util.List;

/**
 * Example profiles used to demonstrate the name generator architecture.
 */
public final class NameProfiles {
    private NameProfiles() {
    }

    public static NameProfile exoticSyllableProfile() {
        return new NameProfile(
                "exotic-syllables",
                "Exotic syllables",
                "Alien-sounding names built from prefix, core and suffix syllables.",
                NameGenerationStrategy.SYLLABLE,
                List.of("va", "zi", "ka", "th"),
                List.of("ra", "sha", "na", "lo", "kor", "vyn"),
                List.of("n", "th", "l", "vex"),
                List.of("vren", "sola", "keth", "naru"));
    }

    public static NameProfile compoundLocationProfile() {
        return new NameProfile(
                "compound-location",
                "Compound location",
                "Names for regions or star systems assembled from multiple lexical parts.",
                NameGenerationStrategy.WORD,
                List.of(),
                List.of(),
                List.of(),
                List.of("Ashen", "Silent", "Iron", "Red", "Null", "Glass", "Hollow", "Vast", "North", "South",
                        "Reach", "Horizon", "Gate", "Meridian", "Keep", "Delta", "Orbit"));
    }

    public static NameProfile technicalTemplateProfile() {
        return new NameProfile(
                "technical-template",
                "Technical template",
                "Machine-readable identifiers assembled from a template.",
                NameGenerationStrategy.TEMPLATE,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "NODE-{NUMBER}-{CODE}",
                null,
                null,
                false,
                25,
                false);
    }
}
