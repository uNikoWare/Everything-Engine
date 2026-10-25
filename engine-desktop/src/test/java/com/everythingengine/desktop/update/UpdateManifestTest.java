package com.everythingengine.desktop.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateManifestTest {
    private static final String CHECKSUM = "a".repeat(64);

    @Test
    void parsesManifestAndReleaseNotes() {
        UpdateManifest manifest = UpdateManifest.parse("""
                version=1.2.3
                installerUrl=https://downloads.example.org/everything-engine.msi
                sha256=%s
                releaseNotes=New universe editor
                """.formatted(CHECKSUM));

        assertEquals("1.2.3", manifest.version());
        assertEquals("https://downloads.example.org/everything-engine.msi", manifest.installerUri().toString());
        assertEquals(CHECKSUM, manifest.sha256());
        assertEquals("New universe editor", manifest.releaseNotes());
    }

    @Test
    void rejectsNonHttpsInstallerAndInvalidChecksum() {
        String httpInstaller = """
                version=1.2.3
                installerUrl=http://downloads.example.org/everything-engine.msi
                sha256=%s
                """.formatted(CHECKSUM);
        assertThrows(IllegalArgumentException.class, () -> UpdateManifest.parse(httpInstaller));

        String invalidChecksum = """
                version=1.2.3
                installerUrl=https://downloads.example.org/everything-engine.msi
                sha256=not-a-checksum
                """;
        assertThrows(IllegalArgumentException.class, () -> UpdateManifest.parse(invalidChecksum));
    }

    @Test
    void comparesNumericVersionComponentsWithoutLexicalOrdering() {
        assertEquals(1, Integer.signum(UpdateManifest.compareVersions("1.10.0", "1.9.9")));
        assertEquals(-1, Integer.signum(UpdateManifest.compareVersions("2.0.0", "10.0.0")));
        assertEquals(0, UpdateManifest.compareVersions("3.4.5", "3.4.5"));
    }
}
