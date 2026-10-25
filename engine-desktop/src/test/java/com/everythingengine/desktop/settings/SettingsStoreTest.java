package com.everythingengine.desktop.settings;

import com.everythingengine.desktop.diagnostics.DiagnosticService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsLanguageSelection() throws Exception {
        Path settingsFile = temporaryDirectory.resolve("settings.properties");
        SettingsStore settings = new SettingsStore(new DiagnosticService(), settingsFile);

        assertEquals(Language.ENGLISH, settings.language());
        settings.setLanguage(Language.GERMAN);

        assertEquals(Language.GERMAN,
                new SettingsStore(new DiagnosticService(), settingsFile).language());
    }
}
