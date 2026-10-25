package com.everythingengine.desktop.ui;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.desktop.DesktopContext;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public final class UniversePage implements Page {
    private final DesktopContext context;
    private final VBox root = new VBox(16);
    private final TableView<Entity> entities = new TableView<>();
    private final TableView<Relation> relations = new TableView<>();
    private final Label summary = new Label();

    public UniversePage(DesktopContext context) {
        this.context = context;
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Universe Explorer", "Inspect and save the entities and directed relationships in this universe."));
        summary.getStyleClass().add("muted");
        root.getChildren().add(summary);
        Button save = UiSupport.button("Save Universe", this::saveUniverse);
        Button load = UiSupport.button("Load Universe", this::loadUniverse);
        root.getChildren().add(new javafx.scene.control.ToolBar(save, load,
                new Label("Autosave: " + context.universeFile())));
        configureEntityTable();
        configureRelationTable();
        TitledPane entitySection = new TitledPane("Entities", entities);
        TitledPane relationSection = new TitledPane("Relations", relations);
        entitySection.setExpanded(true);
        relationSection.setExpanded(true);
        VBox.setVgrow(entitySection, Priority.ALWAYS);
        VBox.setVgrow(relationSection, Priority.ALWAYS);
        root.getChildren().addAll(entitySection, relationSection);
        VBox.setVgrow(entitySection, Priority.ALWAYS);
        VBox.setVgrow(relationSection, Priority.ALWAYS);
        root.setPadding(new Insets(26));
        UiSupport.constrained(entities);
        UiSupport.constrained(relations);
        onShown();
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void onShown() {
        entities.getItems().setAll(context.universe().entities());
        relations.getItems().setAll(context.universe().relations());
        summary.setText(context.universe().entities().size() + " " + Localization.text("entities") + " · "
                + context.universe().relations().size() + " " + Localization.text("relations") + " · "
                + Localization.text("Autosaved"));
    }

    private void configureEntityTable() {
        TableColumn<Entity, String> name = UiSupport.column("Name", 190);
        name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<Entity, String> type = UiSupport.column("Type", 140);
        type.setCellValueFactory(cell -> new ReadOnlyStringWrapper(UiSupport.entityType(cell.getValue())));
        TableColumn<Entity, String> id = UiSupport.column("UUID", 320);
        id.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id().toString()));
        entities.getColumns().setAll(List.of(name, type, id));
    }

    private void saveUniverse() {
        FileChooser chooser = universeChooser("Save Universe");
        chooser.setInitialFileName("universe.eeu");
        var file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            context.saveUniverse(file.toPath());
            context.diagnostics().info("Persistence", "Saved universe to " + file);
            new Alert(Alert.AlertType.INFORMATION, Localization.text("Universe saved successfully."))
                    .showAndWait();
        } catch (IOException | RuntimeException exception) {
            context.diagnostics().error("Persistence", "Could not save universe.", exception);
            UiSupport.showError("Could not save universe", exception);
        }
    }

    private void loadUniverse() {
        FileChooser chooser = universeChooser("Load Universe");
        var file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        try {
            context.loadUniverse(file.toPath());
            context.diagnostics().info("Persistence", "Loaded universe from " + file);
            onShown();
            new Alert(Alert.AlertType.INFORMATION, Localization.text("Universe loaded successfully."))
                    .showAndWait();
        } catch (IOException | RuntimeException exception) {
            context.diagnostics().error("Persistence", "Could not load universe.", exception);
            UiSupport.showError("Could not load universe", exception);
        }
    }

    private FileChooser universeChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Localization.text(title));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                Localization.text("Everything Engine Universe (*.eeu)"), "*.eeu"));
        return chooser;
    }

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }

    private void configureRelationTable() {
        TableColumn<Relation, String> type = UiSupport.column("Type", 140);
        type.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().type().key()));
        TableColumn<Relation, String> source = UiSupport.column("Source", 250);
        source.setCellValueFactory(cell -> new ReadOnlyStringWrapper(UiSupport.endpoint(cell.getValue(), true)));
        TableColumn<Relation, String> target = UiSupport.column("Target", 250);
        target.setCellValueFactory(cell -> new ReadOnlyStringWrapper(UiSupport.endpoint(cell.getValue(), false)));
        relations.getColumns().setAll(List.of(type, source, target));
    }
}
