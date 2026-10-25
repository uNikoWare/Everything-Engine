package com.everythingengine.desktop.ui;

import com.everythingengine.desktop.DesktopContext;
import com.everythingengine.desktop.module.ModuleStatus;
import com.everythingengine.desktop.testing.TestRunResult;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class DashboardPage implements Page {
    private final DesktopContext context;
    private final Consumer<String> navigate;
    private final VBox root = new VBox(20);
    private final Label entityCount = new Label("0");
    private final Label relationCount = new Label("0");
    private final Label moduleCount = new Label("0");
    private final Label activeModuleCount = new Label("0");
    private final Label testStatus = new Label("Not Run");
    private final Label universeStatus = new Label("Empty · In memory only");
    private final Label errorStatus = new Label("Application errors: None");

    public DashboardPage(DesktopContext context, Consumer<String> navigate) {
        this.context = context;
        this.navigate = navigate;
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Dashboard", "Engine status and quick access to the current workspace."));
        root.getChildren().add(brandCard());
        root.getChildren().add(statusCard());
        root.getChildren().add(statistics());
        root.getChildren().add(quickAccess());
        onShown();
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void onShown() {
        entityCount.setText(Integer.toString(context.universe().entities().size()));
        relationCount.setText(Integer.toString(context.universe().relations().size()));
        moduleCount.setText(Integer.toString(context.modules().modules().size()));
        activeModuleCount.setText(Long.toString(context.modules().modules().stream()
                .filter(module -> module.status() == ModuleStatus.ACTIVE)
                .count()));
        universeStatus.setText(context.universe().entities().isEmpty()
                ? "Empty · In memory only"
                : "Initialized · In memory only");
        errorStatus.setText(context.diagnostics().errorCount() == 0
                ? "Application errors: None"
                : "Application errors: " + context.diagnostics().errorCount());
        TestRunResult lastRun = context.lastTestResult();
        testStatus.setText(lastRun == null ? "Not Run"
                : lastRun.passed() + " passed · " + lastRun.failed() + " failed");
    }

    private Node statusCard() {
        Label title = new Label("ENGINE STATUS");
        title.getStyleClass().add("eyebrow");
        Label core = new Label("Core initialized");
        Label universe = new Label();
        universe.textProperty().bind(universeStatus.textProperty());
        Label persistence = new Label("Persistence: Not configured");
        persistence.getStyleClass().add("muted");
        VBox card = new VBox(10, title, core, universe, errorStatus, persistence);
        card.getStyleClass().add("card");
        return card;
    }

    private Node brandCard() {
        ImageView logo = new ImageView(new Image(
                getClass().getResource("/images/everything-engine-logo.png").toExternalForm()));
        logo.setFitWidth(170);
        logo.setFitHeight(170);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        Label title = new Label("Everything Engine");
        title.getStyleClass().add("section-title");
        Label description = new Label("A foundation for building and exploring science-fiction universes.");
        description.getStyleClass().add("page-subtitle");
        description.setWrapText(true);
        VBox copy = new VBox(10, title, description);
        copy.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox card = new HBox(22, logo, copy);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.getStyleClass().add("brand-card");
        return card;
    }

    private Node statistics() {
        HBox cards = new HBox(14,
                statCard("ENTITIES", entityCount),
                statCard("RELATIONS", relationCount),
                statCard("REGISTERED MODULES", moduleCount),
                statCard("ACTIVE MODULES", activeModuleCount),
                statCard("LAST TEST RUN", testStatus));
        for (Node node : cards.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
            ((VBox) node).setMaxWidth(Double.MAX_VALUE);
        }
        return cards;
    }

    private Node statCard(String title, Label value) {
        Label caption = new Label(title);
        caption.getStyleClass().add("eyebrow");
        value.getStyleClass().add("stat-value");
        VBox card = new VBox(12, caption, value);
        card.getStyleClass().add("card");
        return card;
    }

    private Node quickAccess() {
        Label title = new Label("Quick access");
        title.getStyleClass().add("section-title");
        HBox actions = new HBox(10,
                quickButton("Explore entities", "Entities"),
                quickButton("View relations", "Relations"),
                quickButton("Manage modules", "Modules"),
                quickButton("Run tests", "Testing"));
        VBox box = new VBox(12, title, actions);
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }

    private Button quickButton(String label, String page) {
        Button button = UiSupport.button(label, () -> navigate.accept(page));
        button.getStyleClass().add("primary-button");
        return button;
    }
}
