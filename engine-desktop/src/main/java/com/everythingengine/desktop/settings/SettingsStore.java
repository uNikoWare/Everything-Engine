package com.everythingengine.desktop.settings;

import com.everythingengine.desktop.diagnostics.DiagnosticService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Locale;
import java.util.Objects;

public final class SettingsStore {
    private final Path settingsFile;
    private final DiagnosticService diagnostics;
    private Theme theme = Theme.DARK;

    public SettingsStore(DiagnosticService diagnostics) {
        this.diagnostics = diagnostics;
        this.settingsFile = settingsPath();
        load();
    }

    public Theme theme() {
        return theme;
    }

    public Path settingsFile() {
        return settingsFile;
    }

    public void setTheme(Theme theme) throws IOException {
        this.theme = Objects.requireNonNull(theme, "theme");
        Properties properties = new Properties();
        properties.setProperty("theme", theme.name());
        Files.createDirectories(settingsFile.getParent());
        try (OutputStream output = Files.newOutputStream(settingsFile)) {
            properties.store(output, "Everything Engine settings");
        }
    }

    private void load() {
        if (!Files.exists(settingsFile)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsFile)) {
            properties.load(input);
            theme = Theme.valueOf(properties.getProperty("theme", "DARK").toUpperCase(Locale.ROOT));
        } catch (IOException | IllegalArgumentException exception) {
            diagnostics.warning("Settings", "Could not load settings; using defaults: " + exception.getMessage());
            theme = Theme.DARK;
        }
    }

    private Path settingsPath() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "EverythingEngine", "settings.properties");
        }
        return Path.of(System.getProperty("user.home"), ".config", "everything-engine", "settings.properties");
    }
}
