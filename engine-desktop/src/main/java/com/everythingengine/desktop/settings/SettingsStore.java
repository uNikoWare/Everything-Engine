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
    private String updateManifestUrl = "";

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

    public String updateManifestUrl() {
        return updateManifestUrl;
    }

    public void setTheme(Theme theme) throws IOException {
        this.theme = Objects.requireNonNull(theme, "theme");
        save();
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
            updateManifestUrl = properties.getProperty("update.manifest.url", "").trim();
            if (!updateManifestUrl.isEmpty()) {
                UpdateClient.validateHttpsUri(updateManifestUrl);
            }
        } catch (IOException | IllegalArgumentException exception) {
            diagnostics.warning("Settings", "Could not load settings; using defaults: " + exception.getMessage());
            theme = Theme.DARK;
            updateManifestUrl = "";
        }
    }

    private void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("theme", theme.name());
        properties.setProperty("update.manifest.url", updateManifestUrl);
        Files.createDirectories(settingsFile.getParent());
        try (OutputStream output = Files.newOutputStream(settingsFile)) {
            properties.store(output, "Everything Engine settings");
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
