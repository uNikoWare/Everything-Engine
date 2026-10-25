package com.everythingengine.desktop.ui;

import com.everythingengine.core.attribute.AttributeKey;
import com.everythingengine.core.attribute.AttributeSnapshot;
import com.everythingengine.core.entity.BasicEntity;
import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.name.NameGenerator;
import com.everythingengine.core.name.NameProfiles;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.desktop.DesktopContext;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.control.SplitPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EntitiesPage implements Page {
    private final DesktopContext context;
    private final VBox root = new VBox(14);
    private final TableView<Entity> entities = new TableView<>();
    private final TableView<AttributeSnapshot> attributes = new TableView<>();
    private final TableView<Relation> relations = new TableView<>();
    private final ObservableList<Entity> entitySource = FXCollections.observableArrayList();
    private final FilteredList<Entity> filteredEntities = new FilteredList<>(entitySource);
    private final TextField search = new TextField();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final Label selectedName = new Label("Select an entity");
    private final Label selectedId = new Label();
    private final Label attributeEmpty = new Label();
    private final NameGenerator nameGenerator = new NameGenerator(202406L);
    private Entity selectedEntity;

    public EntitiesPage(DesktopContext context) {
        this.context = context;
        nameGenerator.registerProfile(NameProfiles.exoticSyllableProfile());
        nameGenerator.registerProfile(NameProfiles.compoundLocationProfile());
        nameGenerator.registerProfile(NameProfiles.technicalTemplateProfile());
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Entity Explorer", "Find, inspect and manage registered entities and their attributes."));
        createEntityTable();
        createAttributeTable();
        createRelationTable();
        ToolBar toolbar = new ToolBar();
        search.setPromptText("Search name or UUID");
        search.textProperty().addListener((observable, before, after) -> updateFilter());
        typeFilter.getItems().add("All Types");
        typeFilter.setValue("All Types");
        typeFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : Localization.text(value);
            }

            @Override
            public String fromString(String value) {
                return Localization.text("All Types").equals(value) ? "All Types" : value;
            }
        });
        typeFilter.setOnAction(event -> updateFilter());
        toolbar.getItems().addAll(search, typeFilter,
                UiSupport.button("New Entity", this::createEntity),
                UiSupport.button("Generate Name", this::generateNameForSelection),
                UiSupport.button("Rename", this::renameEntity),
                UiSupport.button("Delete", this::deleteEntity));
        root.getChildren().add(toolbar);

        VBox entityPanel = new VBox(8, new Label("Registered entities"), entities);
        entityPanel.getStyleClass().add("card");
        VBox.setVgrow(entities, Priority.ALWAYS);
        VBox details = detailsPanel();
        SplitPane split = new SplitPane(entityPanel, details);
        split.setDividerPositions(0.46);
        VBox.setVgrow(split, Priority.ALWAYS);
        root.getChildren().add(split);
        UiSupport.constrained(entities);
        UiSupport.constrained(attributes);
        UiSupport.constrained(relations);
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

    public void selectEntity(UUID id) {
        context.universe().findEntity(id).ifPresent(entity -> {
            entities.getSelectionModel().select(entity);
            entities.scrollTo(entity);
        });
    }

    private void createEntityTable() {
        TableColumn<Entity, String> name = UiSupport.column("Name", 150);
        name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<Entity, String> type = UiSupport.column("Type", 110);
        type.setCellValueFactory(cell -> new ReadOnlyStringWrapper(UiSupport.entityType(cell.getValue())));
        TableColumn<Entity, String> id = UiSupport.column("UUID", 260);
        id.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id().toString()));
        entities.getColumns().setAll(List.of(name, type, id));
        entities.setItems(filteredEntities);
        entities.getSelectionModel().selectedItemProperty().addListener(
                (observable, before, after) -> showDetails(after));
    }

    private void createAttributeTable() {
        TableColumn<AttributeSnapshot, String> name = UiSupport.column("Key", 120);
        name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<AttributeSnapshot, String> type = UiSupport.column("Type", 100);
        type.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().type().getSimpleName()));
        TableColumn<AttributeSnapshot, String> value = UiSupport.column("Value", 160);
        value.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().value())));
        attributes.getColumns().setAll(List.of(name, type, value));
    }

    private void createRelationTable() {
        TableColumn<Relation, String> type = UiSupport.column("Type", 120);
        type.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().type().key()));
        TableColumn<Relation, String> direction = UiSupport.column("Direction", 180);
        direction.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().source().name() + " → " + cell.getValue().target().name()));
        relations.getColumns().setAll(List.of(type, direction));
    }

    private VBox detailsPanel() {
        selectedName.getStyleClass().add("section-title");
        selectedId.getStyleClass().add("muted");
        selectedId.setWrapText(true);
        Button copyId = UiSupport.button("Copy UUID", () -> {
            if (selectedEntity != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(selectedEntity.id().toString());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
        attributes.setPrefHeight(150);
        attributeEmpty.getStyleClass().add("muted");
        Button addAttribute = UiSupport.button("Add Attribute", this::addAttribute);
        Button removeAttribute = UiSupport.button("Remove Selected", this::removeAttribute);
        VBox attributeSection = new VBox(6, new Label("Attributes"), attributeEmpty, attributes,
                new ToolBar(addAttribute, removeAttribute));
        relations.setPrefHeight(120);
        VBox relationSection = new VBox(6, new Label("Relations"), relations);
        VBox panel = new VBox(11, selectedName, selectedId, copyId, attributeSection, relationSection);
        panel.getChildren().add(1, new Label("General Information"));
        panel.getStyleClass().add("detail-panel");
        return panel;
    }

    private void refresh() {
        UUID previouslySelected = selectedEntity == null ? null : selectedEntity.id();
        entitySource.setAll(context.universe().entities());
        List<String> types = context.universe().entities().stream()
                .map(Entity::getClass)
                .map(Class::getSimpleName)
                .distinct()
                .sorted()
                .toList();
        String previous = typeFilter.getValue();
        typeFilter.getItems().setAll("All Types");
        typeFilter.getItems().addAll(types);
        typeFilter.setValue(typeFilter.getItems().contains(previous) ? previous : "All Types");
        updateFilter();
        if (previouslySelected != null) {
            context.universe().findEntity(previouslySelected).ifPresentOrElse(entity -> {
                entities.getSelectionModel().select(entity);
                showDetails(entity);
            }, () -> showDetails(null));
        }
    }

    private void updateFilter() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        String type = typeFilter.getValue();
        filteredEntities.setPredicate(entity -> {
            boolean matchesType = type == null || type.equals("All Types")
                    || entity.getClass().getSimpleName().equals(type);
            boolean matchesSearch = query.isEmpty()
                    || entity.name().toLowerCase().contains(query)
                    || entity.id().toString().toLowerCase().contains(query);
            return matchesType && matchesSearch;
        });
    }

    private void showDetails(Entity entity) {
        selectedEntity = entity;
        if (entity == null) {
            selectedName.setText("Select an entity");
            selectedId.setText("");
            attributes.getItems().clear();
            relations.getItems().clear();
            attributeEmpty.setText("No entity selected.");
            return;
        }
        selectedName.setText(entity.name() + " · " + UiSupport.entityType(entity));
        selectedId.setText(entity.id().toString());
        List<AttributeSnapshot> values = entity.attributes();
        attributes.getItems().setAll(values);
        attributeEmpty.setText(values.isEmpty() ? "No attributes defined." : "");
        relations.getItems().setAll(context.universe().relationsOf(entity));
    }

    private void createEntity() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Localization.text("Create Entity"));
        dialog.setHeaderText(Localization.text("Create a general-purpose entity"));
        dialog.setContentText(Localization.text("Name:"));
        dialog.showAndWait().ifPresent(name -> {
            try {
                context.universe().registerEntity(new BasicEntity(name));
                context.autosaveUniverse();
                context.diagnostics().info("Entities", "Registered entity: " + name);
                refresh();
            } catch (RuntimeException exception) {
                report("Could not create entity", exception);
            }
        });
    }

    private void generateNameForSelection() {
        String generatedName = generateUniqueEntityName();
        if (selectedEntity == null) {
            try {
                context.universe().registerEntity(new BasicEntity(generatedName));
                context.autosaveUniverse();
                context.diagnostics().info("Entities", "Registered generated entity: " + generatedName);
                refresh();
            } catch (RuntimeException exception) {
                report("Could not generate entity name", exception);
            }
            return;
        }

        try {
            selectedEntity.setName(generatedName);
            context.autosaveUniverse();
            context.diagnostics().info("Entities", "Renamed entity " + selectedEntity.id() + " to " + generatedName);
            refresh();
            entities.getSelectionModel().select(selectedEntity);
        } catch (RuntimeException exception) {
            report("Could not rename entity", exception);
        }
    }

    private String generateUniqueEntityName() {
        List<String> existingNames = context.universe().entities().stream()
                .map(Entity::name)
                .toList();

        for (var profile : nameGenerator.profiles()) {
            for (String candidate : nameGenerator.generateMany(profile, 3, true)) {
                if (!existingNames.contains(candidate)) {
                    return candidate;
                }
            }
        }

        return "Generated-Entity-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void renameEntity() {
        if (selectedEntity == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selectedEntity.name());
        dialog.setTitle(Localization.text("Rename Entity"));
        dialog.setHeaderText(Localization.text("Update the entity name"));
        dialog.setContentText(Localization.text("Name:"));
        dialog.showAndWait().ifPresent(name -> {
            try {
                selectedEntity.setName(name);
                context.autosaveUniverse();
                context.diagnostics().info("Entities", "Renamed entity " + selectedEntity.id());
                refresh();
                entities.getSelectionModel().select(selectedEntity);
            } catch (RuntimeException exception) {
                report("Could not rename entity", exception);
            }
        });
    }

    private void deleteEntity() {
        if (selectedEntity == null) {
            return;
        }
        ButtonType delete = new ButtonType(
                Localization.text("OK"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                Localization.text("Delete this entity and its connected relations?"),
                new ButtonType(Localization.text("Cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE),
                delete);
        confirmation.setHeaderText(Localization.text("Delete ") + selectedEntity.name() + "?");
        confirmation.showAndWait().filter(delete::equals).ifPresent(ignored -> {
            try {
                context.universe().removeEntity(selectedEntity.id());
                context.autosaveUniverse();
                context.diagnostics().info("Entities", "Removed entity and connected relations.");
                showDetails(null);
                refresh();
            } catch (RuntimeException exception) {
                report("Could not delete entity", exception);
            }
        });
    }

    private void addAttribute() {
        if (selectedEntity == null) {
            return;
        }
        Dialog<AttributeForm> dialog = new Dialog<>();
        dialog.setTitle(Localization.text("Add Attribute"));
        dialog.setHeaderText(Localization.text("Add a typed attribute"));
        ButtonType add = new ButtonType(
                Localization.text("Add"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(
                Localization.text("Cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(add, cancel);
        TextField key = new TextField();
        TextField value = new TextField();
        ComboBox<String> type = new ComboBox<>();
        type.getItems().setAll("String", "Integer", "Decimal", "Boolean");
        type.setValue("String");
        type.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String value) {
                return Localization.text(value);
            }

            @Override
            public String fromString(String value) {
                return type.getItems().stream()
                        .filter(item -> Localization.text(item).equals(value))
                        .findFirst()
                        .orElse(value);
            }
        });
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.addRow(0, new Label(Localization.text("Key")), key);
        fields.addRow(1, new Label(Localization.text("Type")), type);
        fields.addRow(2, new Label(Localization.text("Value")), value);
        dialog.getDialogPane().setContent(fields);
        Localization.translate(dialog.getDialogPane());
        dialog.setResultConverter(button -> button == add
                ? new AttributeForm(key.getText(), type.getValue(), value.getText())
                : null);
        dialog.showAndWait().ifPresent(form -> {
            try {
                storeAttribute(selectedEntity, form);
                context.autosaveUniverse();
                context.diagnostics().info("Entities", "Updated attribute " + form.key());
                showDetails(selectedEntity);
            } catch (RuntimeException exception) {
                report("Could not add attribute", exception);
            }
        });
    }

    private void storeAttribute(Entity entity, AttributeForm form) {
        switch (form.type()) {
            case "String" -> entity.setAttribute(new AttributeKey<>(form.key(), String.class), form.value());
            case "Integer" -> entity.setAttribute(
                    new AttributeKey<>(form.key(), Integer.class), Integer.valueOf(form.value()));
            case "Decimal" -> entity.setAttribute(
                    new AttributeKey<>(form.key(), Double.class), Double.valueOf(form.value()));
            case "Boolean" -> {
                if (!form.value().equalsIgnoreCase("true") && !form.value().equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("Boolean values must be true or false.");
                }
                entity.setAttribute(new AttributeKey<>(form.key(), Boolean.class), Boolean.valueOf(form.value()));
            }
            default -> throw new IllegalArgumentException("Unsupported attribute type.");
        }
    }

    private void removeAttribute() {
        if (selectedEntity == null || attributes.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        AttributeSnapshot attribute = attributes.getSelectionModel().getSelectedItem();
        try {
            removeAttribute(selectedEntity, attribute.name(), attribute.type());
            context.autosaveUniverse();
            showDetails(selectedEntity);
        } catch (RuntimeException exception) {
            report("Could not remove attribute", exception);
        }
    }

    private void report(String title, RuntimeException exception) {
        context.diagnostics().error("Entities", title, exception);
        UiSupport.showError(title, exception);
    }

    private <T> void removeAttribute(Entity entity, String name, Class<T> type) {
        entity.removeAttribute(new AttributeKey<>(name, type));
    }

    private record AttributeForm(String key, String type, String value) {
    }
}
