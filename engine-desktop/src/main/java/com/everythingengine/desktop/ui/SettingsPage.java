package com.everythingengine.desktop.ui;

import com.everythingengine.desktop.DesktopContext;
import com.everythingengine.desktop.settings.Theme;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public final class SettingsPage implements Page {
    private final DesktopContext context;
    private final Runnable themeChanged;
    private final VBox root = new VBox(18);

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
}
