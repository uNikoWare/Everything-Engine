package com.everythingengine.desktop.settings;

import com.everythingengine.desktop.diagnostics.DiagnosticService;
import com.everythingengine.desktop.update.UpdateClient;

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
    private Language language = Language.ENGLISH;
    private String updateManifestUrl = "";

    public SettingsStore(DiagnosticService diagnostics) {
        this(diagnostics, settingsPath());
    }

    SettingsStore(DiagnosticService diagnostics, Path settingsFile) {
        this.diagnostics = diagnostics;
        this.settingsFile = Objects.requireNonNull(settingsFile, "settingsFile");
        load();
    }

    public Theme theme() {
        return theme;
    }

    public Path settingsFile() {
        return settingsFile;
    }

    public String updateManifestUrl() {
        return updateManifestUrl;
    }

    public Language language() {
        return language;
    }

    public void setTheme(Theme theme) throws IOException {
        Theme previousTheme = this.theme;
        this.theme = Objects.requireNonNull(theme, "theme");
        try {
            save();
        } catch (IOException exception) {
            this.theme = previousTheme;
            throw exception;
        }
    }

    public void setLanguage(Language language) throws IOException {
        Language previousLanguage = this.language;
        this.language = Objects.requireNonNull(language, "language");
        try {
            save();
        } catch (IOException exception) {
            this.language = previousLanguage;
            throw exception;
        }
    }

    public void setUpdateManifestUrl(String url) throws IOException {
        String normalized = Objects.requireNonNull(url, "url").trim();
        if (!normalized.isEmpty()) {
            UpdateClient.validateHttpsUri(normalized);
        }
        String previousUrl = updateManifestUrl;
        updateManifestUrl = normalized;
        try {
            save();
        } catch (IOException exception) {
            updateManifestUrl = previousUrl;
            throw exception;
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
            language = Language.valueOf(properties.getProperty("language", "ENGLISH").toUpperCase(Locale.ROOT));
            updateManifestUrl = properties.getProperty("update.manifest.url", "").trim();
            if (!updateManifestUrl.isEmpty()) {
                UpdateClient.validateHttpsUri(updateManifestUrl);
            }
        } catch (IOException | IllegalArgumentException exception) {
            diagnostics.warning("Settings", "Could not load settings; using defaults: " + exception.getMessage());
            theme = Theme.DARK;
            language = Language.ENGLISH;
            updateManifestUrl = "";
        }
    }

    private void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("theme", theme.name());
        properties.setProperty("language", language.name());
        properties.setProperty("update.manifest.url", updateManifestUrl);
        Files.createDirectories(settingsFile.getParent());
        try (OutputStream output = Files.newOutputStream(settingsFile)) {
            properties.store(output, "Everything Engine settings");
        }
    }

    private static Path settingsPath() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "EverythingEngine", "settings.properties");
        }
        return Path.of(System.getProperty("user.home"), ".config", "everything-engine", "settings.properties");
    }
}
