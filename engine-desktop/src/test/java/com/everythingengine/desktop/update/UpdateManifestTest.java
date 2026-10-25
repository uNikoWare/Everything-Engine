package com.everythingengine.desktop.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateManifestTest {
    private static final String CHECKSUM = "a".repeat(64);

    @Test
    void parsesManifestAndReleaseNotes() {
        UpdateManifest manifest = UpdateManifest.parse("""
                version=v.0.1.2
                installerUrl=https://downloads.example.org/everything-engine.msi
                sha256=%s
                releaseNotes=New universe editor
                """.formatted(CHECKSUM));

        assertEquals("v.0.1.2", manifest.version());
        assertEquals("https://downloads.example.org/everything-engine.msi", manifest.installerUri().toString());
        assertEquals(CHECKSUM, manifest.sha256());
        assertEquals("New universe editor", manifest.releaseNotes());
    }

    @Test
    void rejectsNonHttpsInstallerAndInvalidChecksum() {
        String httpInstaller = """
                version=v.0.1.2
                installerUrl=http://downloads.example.org/everything-engine.msi
                sha256=%s
                """.formatted(CHECKSUM);
        assertThrows(IllegalArgumentException.class, () -> UpdateManifest.parse(httpInstaller));

        String invalidChecksum = """
                version=v.0.1.2
                installerUrl=https://downloads.example.org/everything-engine.msi
                sha256=not-a-checksum
                """;
        assertThrows(IllegalArgumentException.class, () -> UpdateManifest.parse(invalidChecksum));
    }

    @Test
    void comparesNumericVersionComponentsWithoutLexicalOrdering() {
        assertEquals(1, Integer.signum(UpdateManifest.compareVersions("v.0.10.0", "v.0.9.9")));
        assertEquals(-1, Integer.signum(UpdateManifest.compareVersions("v.2.0.0", "v.10.0.0")));
        assertEquals(0, UpdateManifest.compareVersions("v.0.1.1", "v.0.1.1"));
        assertEquals(-1, Integer.signum(UpdateManifest.compareVersions("v.0.1.1_a", "v.0.1.1_b")));
        assertEquals(-1, Integer.signum(UpdateManifest.compareVersions("v.0.1.1_a", "v.0.1.1")));
    }

    @Test
    void extractsNumericPackageVersionWithoutBranchSuffix() {
        assertEquals("v.0.2.1", DesktopVersion.current());
        assertEquals("1.2.1", DesktopVersion.packageVersion(DesktopVersion.current()));
        assertEquals("1.2.0", DesktopVersion.packageVersion("v.0.2.0"));
        assertEquals("1.1.1", DesktopVersion.packageVersion("v.0.1.1"));
        assertEquals("1.1.1", DesktopVersion.packageVersion("v.0.1.1_z"));
        assertEquals("2.0.0", DesktopVersion.packageVersion("v.1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> DesktopVersion.requireValid("0.1.1"));
        assertThrows(IllegalArgumentException.class, () -> DesktopVersion.requireValid("v.0.1.1_A"));
    }
}
