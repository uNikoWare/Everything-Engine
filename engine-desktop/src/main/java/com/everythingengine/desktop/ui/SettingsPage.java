package com.everythingengine.desktop.ui;

import com.everythingengine.desktop.DesktopContext;
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

public final class SettingsPage implements Page {
    private final DesktopContext context;
    private final Runnable themeChanged;
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

    public SettingsPage(DesktopContext context, Runnable themeChanged) {
        this.context = context;
        this.themeChanged = themeChanged;
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Settings", "A small set of persisted preferences for this desktop application."));
        ComboBox<Theme> theme = new ComboBox<>();
        theme.getItems().setAll(Theme.values());
        theme.setValue(context.settings().theme());
        theme.setOnAction(event -> saveTheme(theme.getValue()));
        HBox themeRow = new HBox(16, new Label("Theme"), theme);
        VBox panel = new VBox(14,
                themeRow,
                new Label("Settings file: " + context.settings().settingsFile()),
                new Label("Theme changes are saved to the current user's application data directory."));
        panel.getStyleClass().add("card");
        root.getChildren().add(panel);
        root.getChildren().add(updatePanel());
        root.getChildren().add(new Label(
                "Universes are currently temporary and are not saved. Language and logging-level preferences are not implemented."));
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Settings Error");
            alert.setHeaderText("Could not save settings.");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
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
            updateStatus.setText(updateFeed.getText().isBlank()
                    ? "Automatic update checks are not configured."
                    : "Update feed saved. Select Check for Updates to look for a release.");
            context.diagnostics().info("Updates", "Update feed settings saved.");
        } catch (IOException | IllegalArgumentException exception) {
            reportUpdateError("Could not save update feed", exception);
        }
    }

    private void checkForUpdates() {
        String feed = context.settings().updateManifestUrl();
        if (feed.isBlank()) {
            updateStatus.setText("Save a trusted HTTPS update manifest URL before checking.");
            return;
        }
        checkUpdates.setDisable(true);
        installUpdate.setDisable(true);
        updateProgress.setVisible(true);
        updateStatus.setText("Checking for updates...");
        checkService = new UpdateCheckService(updateClient, feed, DesktopVersion.current());
        checkService.setOnSucceeded(event -> {
            checkUpdates.setDisable(false);
            updateProgress.setVisible(false);
            UpdateCheckResult result = checkService.getValue();
            if (result.updateAvailable()) {
                availableUpdate = result.update().orElseThrow();
                updateStatus.setText("Version " + availableUpdate.version() + " is available."
                        + (availableUpdate.releaseNotes().isBlank() ? "" : "\n" + availableUpdate.releaseNotes()));
                installUpdate.setDisable(WindowsUpdateInstaller.currentApplicationPath().isEmpty());
                if (installUpdate.isDisabled()) {
                    updateStatus.setText(updateStatus.getText()
                            + "\nAutomatic installation is available only from the installed Windows app.");
                }
                context.diagnostics().info("Updates", "Update " + availableUpdate.version() + " is available.");
            } else {
                availableUpdate = null;
                installUpdate.setDisable(true);
                updateStatus.setText("Everything Engine " + result.currentVersion() + " is up to date.");
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
            updateStatus.setText("Update check failed: " + message);
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
        confirmation.setTitle("Install Update");
        confirmation.setHeaderText("Install Everything Engine " + manifest.version() + "?");
        confirmation.setContentText("The verified Windows Installer package will be downloaded. "
                + "The application will close, apply the update, and restart.");
        if (confirmation.showAndWait().filter(javafx.scene.control.ButtonType.OK::equals).isEmpty()) {
            return;
        }

        checkUpdates.setDisable(true);
        installUpdate.setDisable(true);
        updateProgress.setVisible(true);
        updateStatus.setText("Downloading and verifying the installer...");
        Path updatesDirectory = context.settings().settingsFile().getParent().resolve("updates");
        downloadService = new UpdateDownloadService(updateClient, manifest, updatesDirectory);
        downloadService.setOnSucceeded(event -> {
            Path installer = downloadService.getValue();
            try {
                WindowsUpdateInstaller.installAfterApplicationExit(
                        installer, appPath.orElseThrow(), ProcessHandle.current().pid());
                updateStatus.setText("The verified update is ready. Closing to install and restart...");
                context.diagnostics().info("Updates", "Handing the verified MSI to Windows Installer.");
                Alert installed = new Alert(Alert.AlertType.INFORMATION);
                installed.setTitle("Update Ready");
                installed.setHeaderText("Everything Engine will now restart into version " + manifest.version() + ".");
                installed.setContentText("Approve the Windows Installer elevation prompt if Windows requests it.");
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
            updateStatus.setText("Update download failed: " + message);
            context.diagnostics().error("Updates", "Update download or verification failed.", error);
        });
        downloadService.start();
    }

    private void reportUpdateError(String title, Exception exception) {
        updateStatus.setText(title + ": " + exception.getMessage());
        context.diagnostics().error("Updates", title, exception);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Update Error");
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
