package com.everythingengine.core.name;

import com.everythingengine.core.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NameGeneratorTest {
    @Test
    void generatesNamesFromProfilesAndValidatesThem() {
        NameGenerator generator = new NameGenerator(123L);
        NameProfile profile = new NameProfile(
                "syl", "Syllabic", "Alien fragments",
                NameGenerationStrategy.SYLLABLE,
                List.of("va", "zi"),
                List.of("ra", "na", "sh"),
                List.of("th", "n"),
                List.of("vex", "sol", "keth"));

        generator.registerProfile(profile);

        String name = generator.generate(profile);
        assertFalse(name.isBlank());
        assertTrue(generator.validateProfile(profile).isValid());
        NameGenerator sameSeedGenerator = new NameGenerator(123L);
        sameSeedGenerator.registerProfile(profile);
        assertEquals(name, sameSeedGenerator.generate(profile));
    }

    @Test
    void rejectsInvalidProfilesAndUnknownTemplateTokens() {
        NameGenerator generator = new NameGenerator(99L);
        NameProfile invalid = new NameProfile("bad", "Bad", "Error case", NameGenerationStrategy.SYLLABLE,
                List.of(), List.of(), List.of());
        NameProfile brokenTemplate = new NameProfile("bad-template", "Bad template", "Broken",
                NameGenerationStrategy.TEMPLATE, List.of(), List.of(), List.of(), List.of(), "{MYSTERY}");

        assertFalse(generator.validateProfile(invalid).isValid());
        assertThrows(IllegalArgumentException.class, () -> generator.generate(invalid));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(brokenTemplate));
    }

    @Test
    void supportsDuplicateAvoidanceAndConfiguredAttempts() {
        NameGenerator generator = new NameGenerator(9L);
        NameProfile profile = new NameProfile(
                "repeatable", "Repeatable", "Repeatable generation",
                NameGenerationStrategy.WORD,
                List.of(), List.of(), List.of(),
                List.of("Aster", "Aster", "Vast", "Vale", "Nexus"));

        List<String> names = generator.generateMany(profile, 3, true);
        assertEquals(3, names.size());
        assertEquals(3, new java.util.LinkedHashSet<>(names).size());
        assertThrows(IllegalStateException.class, () -> generator.generateMany(new NameProfile(
                "tight", "Tight", "Tight attempts", NameGenerationStrategy.TEMPLATE,
                List.of(), List.of(), List.of(), List.of(), "fixed", null, null, false, 1, false), 2, true));
    }
}
