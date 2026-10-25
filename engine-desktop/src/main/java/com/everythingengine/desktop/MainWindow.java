package com.everythingengine.desktop;

import com.everythingengine.desktop.settings.Theme;
import com.everythingengine.desktop.ui.DashboardPage;
import com.everythingengine.desktop.ui.EntitiesPage;
import com.everythingengine.desktop.ui.LogsPage;
import com.everythingengine.desktop.ui.ModulesPage;
import com.everythingengine.desktop.ui.NameGenerationPage;
import com.everythingengine.desktop.ui.Page;
import com.everythingengine.desktop.ui.RelationsPage;
import com.everythingengine.desktop.ui.SettingsPage;
import com.everythingengine.desktop.ui.TestingPage;
import com.everythingengine.desktop.ui.UniversePage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MainWindow {
    private final DesktopContext context;
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, Button> navigationButtons = new LinkedHashMap<>();
    private final Label status = new Label("Ready · Universe is in memory");
    private final Label globalStatus = new Label();
    private Scene scene;

    public MainWindow(DesktopContext context) {
        this.context = context;
        Consumer<String> navigate = this::showPage;
        pages.put("Dashboard", new DashboardPage(context, navigate));
        pages.put("Universe", new UniversePage(context));
        pages.put("Entities", new EntitiesPage(context));
        pages.put("Relations", new RelationsPage(context, id -> {
            showPage("Entities");
            ((EntitiesPage) pages.get("Entities")).selectEntity(id);
        }));
        pages.put("Modules", new ModulesPage(context));
        pages.put("Name Generation", new NameGenerationPage());
        pages.put("Testing", new TestingPage(context));
        pages.put("Logs", new LogsPage(context));
        pages.put("Settings", new SettingsPage(context, this::applyTheme));
        root.setTop(createHeader());
        root.setLeft(createNavigation());
        root.setCenter(content);
        root.setBottom(createStatusBar());
        root.getStyleClass().add("app-root");
        showPage("Dashboard");
    }

    public BorderPane root() {
        return root;
    }

    public void applyTheme(Scene scene) {
        this.scene = scene;
        scene.getRoot().getStyleClass().removeAll("dark-theme", "light-theme");
        scene.getRoot().getStyleClass().add(
                context.settings().theme() == Theme.DARK ? "dark-theme" : "light-theme");
    }

    private void applyTheme() {
        if (scene != null) {
            applyTheme(scene);
        }
    }

    private Node createHeader() {
        ImageView logo = new ImageView(new Image(
                getClass().getResource("/icons/everything-engine-icon.png").toExternalForm()));
        logo.setFitWidth(44);
        logo.setFitHeight(44);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        Label name = new Label("EVERYTHING ENGINE");
        name.getStyleClass().add("brand");
        Label edition = new Label("DEVELOPMENT WORKSPACE");
        edition.getStyleClass().add("eyebrow");
        VBox branding = new VBox(4, name, edition);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        globalStatus.getStyleClass().add("status-pill");
        HBox header = new HBox(14, logo, branding, spacer, globalStatus);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.getStyleClass().add("top-bar");
        return header;
    }

    private Node createNavigation() {
        VBox navigation = new VBox(6);
        navigation.setPadding(new Insets(20, 12, 20, 12));
        navigation.setPrefWidth(200);
        navigation.getStyleClass().add("navigation");
        Label section = new Label("WORKSPACE");
        section.getStyleClass().add("eyebrow");
        navigation.getChildren().add(section);
        for (String pageName : pages.keySet()) {
            Button button = new Button(pageName);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.getStyleClass().add("navigation-button");
            button.setOnAction(event -> showPage(pageName));
            navigationButtons.put(pageName, button);
            navigation.getChildren().add(button);
        }
        return navigation;
    }

    private Node createStatusBar() {
        HBox footer = new HBox(status);
        footer.setPadding(new Insets(9, 18, 9, 18));
        footer.getStyleClass().add("status-bar");
        return footer;
    }

    private void showPage(String pageName) {
        Page page = pages.get(pageName);
        if (page == null) {
            return;
        }
        content.getChildren().setAll(page.node());
        page.onShown();
        long errors = context.diagnostics().errorCount();
        globalStatus.setText(errors == 0 ? "CORE READY · NO ERRORS" : "CORE READY · " + errors + " ERRORS");
        navigationButtons.forEach((name, button) ->
                button.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),
                        name.equals(pageName)));
        status.setText(pageName + " · In-memory universe · No persistence configured");
    }
}
