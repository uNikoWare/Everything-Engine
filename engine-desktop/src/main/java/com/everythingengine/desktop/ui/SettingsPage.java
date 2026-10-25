package com.everythingengine.desktop.ui;

import com.everythingengine.desktop.DesktopContext;
import com.everythingengine.desktop.settings.Language;
import com.everythingengine.desktop.settings.Theme;
import com.everythingengine.desktop.update.DesktopVersion;
import com.everythingengine.desktop.update.UpdateCheckResult;
import com.everythingengine.desktop.update.UpdateCheckService;
import com.everythingengine.desktop.update.UpdateClient;
import com.everythingengine.desktop.update.UpdateDownloadService;
import com.everythingengine.desktop.update.UpdateManifest;
import com.everythingengine.desktop.update.WindowsUpdateInstaller;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javafx.util.StringConverter;

public final class SettingsPage implements Page {
    private final DesktopContext context;
    private final Runnable themeChanged;
    private final Runnable languageChanged;
    private final VBox root = new VBox(18);
    private final TextField updateFeed = new TextField();
    private final Button checkUpdates = new Button("Check for Updates");
    private final Button installUpdate = new Button("Download and Install Update");
    private final Label updateStatus = new Label();
    private final ProgressIndicator updateProgress = new ProgressIndicator();
    private final UpdateClient updateClient = new UpdateClient();
    private UpdateManifest availableUpdate;
    private UpdateCheckService checkService;
    private UpdateDownloadService downloadService;

    public SettingsPage(DesktopContext context, Runnable themeChanged, Runnable languageChanged) {
        this.context = context;
        this.themeChanged = themeChanged;
        this.languageChanged = languageChanged;
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Settings", "A small set of persisted preferences for this desktop application."));
        ComboBox<Theme> theme = new ComboBox<>();
        theme.getItems().setAll(Theme.values());
        theme.setValue(context.settings().theme());
        theme.setConverter(enumConverter());
        ComboBox<Language> language = new ComboBox<>();
        language.getItems().setAll(Language.values());
        language.setValue(context.settings().language());
        language.setConverter(new StringConverter<>() {
            @Override
            public String toString(Language value) {
                return value == null ? "" : Localization.text(
                        value == Language.GERMAN ? "German" : "English", context.settings().language());
            }

            @Override
            public Language fromString(String value) {
                return Language.valueOf(value.toUpperCase(java.util.Locale.ROOT));
            }
        });
        theme.setOnAction(event -> saveTheme(theme.getValue()));
        language.setOnAction(event -> saveLanguage(language.getValue(), language));
        HBox themeRow = new HBox(16, new Label("Theme"), theme);
        HBox languageRow = new HBox(16, new Label("Language"), language);
        VBox panel = new VBox(14,
                themeRow,
                languageRow,
                new Label("Settings file: " + context.settings().settingsFile()),
                new Label("Theme and language preferences are saved to the current user's application data directory."),
                new Label("Universe autosave file: " + context.universeFile()));
        panel.getStyleClass().add("card");
        root.getChildren().add(panel);
        root.getChildren().add(updatePanel());
        Localization.translate(root, context.settings().language());
    }

    @Override
    public Node node() {
        return root;
    }

    private void saveTheme(Theme theme) {
        try {
            context.settings().setTheme(theme);
            themeChanged.run();
            context.diagnostics().info("Settings", "Theme changed to " + theme);
        } catch (IOException exception) {
            context.diagnostics().error("Settings", "Could not save theme settings.", exception);
            UiSupport.showError("Settings Error", exception);
        }
    }

    private void saveLanguage(Language language, ComboBox<Language> selector) {
        try {
            context.settings().setLanguage(language);
            context.diagnostics().info("Settings", "Language changed to " + language);
            languageChanged.run();
        } catch (IOException exception) {
            selector.setValue(context.settings().language());
            context.diagnostics().error("Settings", "Could not save language settings.", exception);
            UiSupport.showError("Settings Error", new IllegalStateException(
                    "Could not save language settings: " + exception.getMessage(), exception));
        }
    }

    private StringConverter<Theme> enumConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Theme value) {
                return value == null ? "" : Localization.text(value.name(), context.settings().language());
            }

            @Override
            public Theme fromString(String value) {
                return Theme.valueOf(value);
            }
        };
    }

    private VBox updatePanel() {
        Label version = new Label("Installed version: " + DesktopVersion.current());
        updateFeed.setPromptText("https://example.org/everything-engine/update.properties");
        updateFeed.setText(context.settings().updateManifestUrl());
        updateFeed.setPrefColumnCount(48);
        HBox.setHgrow(updateFeed, Priority.ALWAYS);
        Button saveFeed = UiSupport.button("Save Update Feed", this::saveUpdateFeed);
        checkUpdates.setOnAction(event -> checkForUpdates());
        installUpdate.setDisable(true);
        installUpdate.setOnAction(event -> downloadAndInstall());
        updateProgress.setPrefSize(22, 22);
        updateProgress.setVisible(false);
        updateStatus.setText(context.settings().updateManifestUrl().isBlank()
                ? "Configure a trusted HTTPS update manifest to check for releases."
                : "Updates are checked only when requested.");
        updateStatus.setWrapText(true);
        HBox urlRow = new HBox(8, updateFeed, saveFeed);
        VBox panel = new VBox(12,
                new Label("Application updates"),
                version,
                new Label("Update manifest URL (HTTPS)"),
                urlRow,
                new HBox(10, checkUpdates, installUpdate, updateProgress),
                updateStatus);
        panel.getStyleClass().add("card");
        return panel;
    }

    private void saveUpdateFeed() {
        try {
            context.settings().setUpdateManifestUrl(updateFeed.getText());
            availableUpdate = null;
            installUpdate.setDisable(true);
            updateStatus.setText(Localization.text(updateFeed.getText().isBlank()
                    ? "Automatic update checks are not configured."
                    : "Update feed saved. Select Check for Updates to look for a release."));
            context.diagnostics().info("Updates", "Update feed settings saved.");
        } catch (IOException | IllegalArgumentException exception) {
            reportUpdateError("Could not save update feed", exception);
        }
    }

    private void checkForUpdates() {
        String feed = context.settings().updateManifestUrl();
        if (feed.isBlank()) {
            updateStatus.setText(Localization.text("Save a trusted HTTPS update manifest URL before checking."));
            return;
        }
        checkUpdates.setDisable(true);
        installUpdate.setDisable(true);
        updateProgress.setVisible(true);
        updateStatus.setText(Localization.text("Checking for updates..."));
        checkService = new UpdateCheckService(updateClient, feed, DesktopVersion.current());
        checkService.setOnSucceeded(event -> {
            checkUpdates.setDisable(false);
            updateProgress.setVisible(false);
            UpdateCheckResult result = checkService.getValue();
            if (result.updateAvailable()) {
                availableUpdate = result.update().orElseThrow();
                updateStatus.setText(Localization.text("Version ") + availableUpdate.version()
                        + Localization.text(" is available.")
                        + (availableUpdate.releaseNotes().isBlank() ? "" : "\n" + availableUpdate.releaseNotes()));
                installUpdate.setDisable(WindowsUpdateInstaller.currentApplicationPath().isEmpty());
                if (installUpdate.isDisabled()) {
                    updateStatus.setText(updateStatus.getText()
                            + "\n" + Localization.text(
                                    "Automatic installation is available only from the installed Windows app."));
                }
                context.diagnostics().info("Updates", "Update " + availableUpdate.version() + " is available.");
            } else {
                availableUpdate = null;
                installUpdate.setDisable(true);
                updateStatus.setText("Everything Engine " + result.currentVersion()
                        + Localization.text(" is up to date."));
                context.diagnostics().info("Updates", "No newer release is available.");
            }
        });
        checkService.setOnFailed(event -> {
            checkUpdates.setDisable(false);
            updateProgress.setVisible(false);
            Throwable error = checkService.getException();
            String message = error == null || error.getMessage() == null
                    ? "Could not check for updates."
                    : error.getMessage();
            updateStatus.setText(Localization.text("Update check failed: ") + message);
            context.diagnostics().error("Updates", "Update check failed.", error);
        });
        checkService.start();
    }

    private void downloadAndInstall() {
        UpdateManifest manifest = availableUpdate;
        Optional<Path> appPath = WindowsUpdateInstaller.currentApplicationPath();
        if (manifest == null || appPath.isEmpty()) {
            installUpdate.setDisable(true);
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(Localization.text("Install Update"));
        confirmation.setHeaderText(Localization.text("Install Everything Engine ") + manifest.version() + "?");
        confirmation.setContentText(Localization.text("The verified Windows Installer package will be downloaded. "
                + "The application will close, apply the update, and restart."));
        if (confirmation.showAndWait().filter(javafx.scene.control.ButtonType.OK::equals).isEmpty()) {
            return;
        }

        checkUpdates.setDisable(true);
        installUpdate.setDisable(true);
        updateProgress.setVisible(true);
        updateStatus.setText(Localization.text("Downloading and verifying the installer..."));
        Path updatesDirectory = context.settings().settingsFile().getParent().resolve("updates");
        downloadService = new UpdateDownloadService(updateClient, manifest, updatesDirectory);
        downloadService.setOnSucceeded(event -> {
            Path installer = downloadService.getValue();
            try {
                WindowsUpdateInstaller.installAfterApplicationExit(
                        installer, appPath.orElseThrow(), ProcessHandle.current().pid());
                updateStatus.setText(Localization.text("The verified update is ready. Closing to install and restart..."));
                context.diagnostics().info("Updates", "Handing the verified MSI to Windows Installer.");
                Alert installed = new Alert(Alert.AlertType.INFORMATION);
                installed.setTitle(Localization.text("Update Ready"));
                installed.setHeaderText(Localization.text(
                        "Everything Engine will now restart into version ") + manifest.version() + ".");
                installed.setContentText(Localization.text(
                        "Approve the Windows Installer elevation prompt if Windows requests it."));
                installed.showAndWait();
                Platform.exit();
            } catch (IOException | RuntimeException exception) {
                updateProgress.setVisible(false);
                checkUpdates.setDisable(false);
                installUpdate.setDisable(false);
                reportUpdateError("Could not start the Windows update installer", exception);
            }
        });
        downloadService.setOnFailed(event -> {
            updateProgress.setVisible(false);
            checkUpdates.setDisable(false);
            installUpdate.setDisable(false);
            Throwable error = downloadService.getException();
            String message = error == null || error.getMessage() == null
                    ? "Could not download or verify the update."
                    : error.getMessage();
            updateStatus.setText(Localization.text("Update download failed: ") + message);
            context.diagnostics().error("Updates", "Update download or verification failed.", error);
        });
        downloadService.start();
    }

    private void reportUpdateError(String title, Exception exception) {
        updateStatus.setText(Localization.text(title) + ": " + Localization.text(exception.getMessage()));
        context.diagnostics().error("Updates", title, exception);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(Localization.text("Update Error"));
        alert.setHeaderText(Localization.text(title));
        alert.setContentText(Localization.text(exception.getMessage()));
        alert.showAndWait();
    }
}
