package com.everythingengine.desktop.ui;

import com.everythingengine.desktop.DesktopContext;
import com.everythingengine.desktop.module.ModuleRegistration;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public final class ModulesPage implements Page {
    private final DesktopContext context;
    private final VBox root = new VBox(16);
    private final TableView<ModuleRegistration> modules = new TableView<>();
    private final Label details = new Label("Select a module to inspect its metadata.");
    private final Label extensionStatus = new Label();

    public ModulesPage(DesktopContext context) {
        this.context = context;
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Module Manager", "Review explicitly registered application components and extension availability."));
        createTable();
        modules.getSelectionModel().selectedItemProperty().addListener(
                (observable, before, selected) -> showDetails(selected));
        modules.setPrefHeight(350);
        extensionStatus.getStyleClass().add("warning-text");
        details.setWrapText(true);
        details.getStyleClass().add("muted");
        VBox panel = new VBox(12, new Label("Registered components"), modules, extensionStatus, details);
        panel.getStyleClass().add("card");
        root.getChildren().add(panel);
        UiSupport.constrained(modules);
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
        TableColumn<ModuleRegistration, String> name = UiSupport.column("Module", 210);
        name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().descriptor().name()));
        TableColumn<ModuleRegistration, String> id = UiSupport.column("ID", 220);
        id.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().descriptor().id()));
        TableColumn<ModuleRegistration, String> version = UiSupport.column("Version", 90);
        version.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().descriptor().version()));
        TableColumn<ModuleRegistration, String> status = UiSupport.column("Status", 110);
        status.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().status().name()));
        modules.getColumns().setAll(List.of(name, id, version, status));
    }

    private void refresh() {
        modules.getItems().setAll(context.modules().modules());
        extensionStatus.setText("Built-in core modules are registered. Dynamic loading and activation for extension modules remain unimplemented.");
        if (!modules.getItems().isEmpty() && modules.getSelectionModel().getSelectedItem() == null) {
            modules.getSelectionModel().selectFirst();
        }
    }

    private void showDetails(ModuleRegistration module) {
        if (module == null) {
            details.setText("No module selected.");
            return;
        }
        var descriptor = module.descriptor();
        details.setText(Localization.text(descriptor.name()) + " · " + descriptor.version() + "\n"
                + Localization.text(descriptor.description()) + "\n" + Localization.text("ID: ") + descriptor.id()
                + "\n" + Localization.text("Status: ") + Localization.text(module.status().name())
                + "\n" + Localization.text("Dependencies: ")
                + (descriptor.dependencies().isEmpty() ? Localization.text("None") : descriptor.dependencies())
                + "\n" + Localization.text("Capabilities: ")
                + (descriptor.capabilities().isEmpty() ? Localization.text("None") : descriptor.capabilities()));
    }
}
