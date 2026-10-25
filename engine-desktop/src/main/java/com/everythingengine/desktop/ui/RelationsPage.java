package com.everythingengine.desktop.ui;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.desktop.DesktopContext;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class RelationsPage implements Page {
    private final DesktopContext context;
    private final Consumer<UUID> openEntity;
    private final VBox root = new VBox(14);
    private final TableView<Relation> table = new TableView<>();
    private final ObservableList<Relation> relationSource = FXCollections.observableArrayList();
    private final FilteredList<Relation> filtered = new FilteredList<>(relationSource);
    private final TextField sourceFilter = new TextField();
    private final TextField targetFilter = new TextField();
    private final TextField typeFilter = new TextField();

    public RelationsPage(DesktopContext context, Consumer<UUID> openEntity) {
        this.context = context;
        this.openEntity = openEntity;
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Relation Explorer", "Inspect directed relationships between registered entities."));
        createTable();
        sourceFilter.setPromptText("Source name or UUID");
        targetFilter.setPromptText("Target name or UUID");
        typeFilter.setPromptText("Relation type");
        sourceFilter.textProperty().addListener((observable, before, after) -> applyFilter());
        targetFilter.textProperty().addListener((observable, before, after) -> applyFilter());
        typeFilter.textProperty().addListener((observable, before, after) -> applyFilter());
        ToolBar toolbar = new ToolBar(sourceFilter, targetFilter, typeFilter,
                UiSupport.button("Add Relation", this::addRelation),
                UiSupport.button("Remove Selected", this::removeRelation),
                UiSupport.button("Open Source", () -> openSelected(true)),
                UiSupport.button("Open Target", () -> openSelected(false)));
        root.getChildren().add(toolbar);
        VBox panel = new VBox(8, new Label("Registered relations"), table);
        panel.getStyleClass().add("card");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(panel, Priority.ALWAYS);
        root.getChildren().add(panel);
        UiSupport.constrained(table);
        refresh();
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void onShown() {
        refresh();
    }

    private void createTable() {
        TableColumn<Relation, String> source = UiSupport.column("Source", 240);
        source.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().source().name() + " · " + cell.getValue().source().id()));
        TableColumn<Relation, String> target = UiSupport.column("Target", 240);
        target.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().target().name() + " · " + cell.getValue().target().id()));
        TableColumn<Relation, String> type = UiSupport.column("Relation Type", 170);
        type.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().type().key()));
        TableColumn<Relation, String> id = UiSupport.column("Relation UUID", 280);
        id.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id().toString()));
        table.getColumns().setAll(List.of(source, target, type, id));
        table.setItems(filtered);
    }

    private void refresh() {
        relationSource.setAll(context.universe().relations());
        applyFilter();
    }

    private void applyFilter() {
        String source = normalized(sourceFilter.getText());
        String target = normalized(targetFilter.getText());
        String type = normalized(typeFilter.getText());
        filtered.setPredicate(relation ->
                matches(relation.source(), source)
                        && matches(relation.target(), target)
                        && relation.type().key().toLowerCase().contains(type));
    }

    private boolean matches(Entity entity, String query) {
        return query.isEmpty() || entity.name().toLowerCase().contains(query)
                || entity.id().toString().toLowerCase().contains(query);
    }

    private String normalized(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private void addRelation() {
        List<Entity> registered = context.universe().entities();
        if (registered.isEmpty()) {
            showError("Register at least one entity before creating a relation.");
            return;
        }
        Dialog<RelationForm> dialog = new Dialog<>();
        dialog.setTitle(Localization.text("Add Relation"));
        dialog.setHeaderText(Localization.text("Create a directed relationship"));
        javafx.scene.control.ButtonType add =
                new javafx.scene.control.ButtonType(
                        Localization.text("Add"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType cancel =
                new javafx.scene.control.ButtonType(
                        Localization.text("Cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(add, cancel);
        ComboBox<Entity> source = new ComboBox<>();
        ComboBox<Entity> target = new ComboBox<>();
        source.getItems().setAll(registered);
        target.getItems().setAll(registered);
        source.setValue(registered.getFirst());
        target.setValue(registered.getFirst());
        source.setCellFactory(ignored -> entityCell());
        target.setCellFactory(ignored -> entityCell());
        source.setButtonCell(entityCell());
        target.setButtonCell(entityCell());
        TextField relationType = new TextField();
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.addRow(0, new Label(Localization.text("Source")), source);
        fields.addRow(1, new Label(Localization.text("Target")), target);
        fields.addRow(2, new Label(Localization.text("Type key")), relationType);
        dialog.getDialogPane().setContent(fields);
        Localization.translate(dialog.getDialogPane());
        dialog.setResultConverter(button -> button == add
                ? new RelationForm(source.getValue(), target.getValue(), relationType.getText())
                : null);
        dialog.showAndWait().ifPresent(form -> {
            try {
                context.universe().addRelation(new Relation(
                        form.source(), form.target(), new com.everythingengine.core.relation.RelationType(form.type())));
                context.autosaveUniverse();
                context.diagnostics().info("Relations", "Registered relation type " + form.type());
                refresh();
            } catch (RuntimeException exception) {
                context.diagnostics().error("Relations", "Could not add relation", exception);
                UiSupport.showError("Could not add relation", exception);
            }
        });
    }

    private javafx.scene.control.ListCell<Entity> entityCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Entity entity, boolean empty) {
                super.updateItem(entity, empty);
                setText(empty || entity == null ? null : UiSupport.entityLabel(entity));
            }
        };
    }

    private void removeRelation() {
        Relation selected = table.getSelectionModel().getSelectedItem();
        if (selected != null && context.universe().removeRelation(selected.id())) {
            try {
                context.autosaveUniverse();
                context.diagnostics().info("Relations", "Removed relation " + selected.id());
                refresh();
            } catch (RuntimeException exception) {
                context.diagnostics().error("Relations", "Could not save the removed relation.", exception);
                UiSupport.showError("Could not save universe", exception);
            }
        }
    }

    private void openSelected(boolean source) {
        Relation selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openEntity.accept((source ? selected.source() : selected.target()).id());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, Localization.text(message));
        alert.setHeaderText(Localization.text("Relation not available"));
        alert.showAndWait();
    }

    private record RelationForm(Entity source, Entity target, String type) {
    }
}
