package com.everythingengine.desktop.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import com.everythingengine.desktop.DesktopContext;

import java.util.List;
import java.util.function.Supplier;

public final class GenerationPage implements Page {
    private final DesktopContext context;
    private final ComboBox<GenerationOption> generatorSelector = new ComboBox<>();
    private final StackPane generatorContent = new StackPane();
    private final VBox root = new VBox(16);

    public GenerationPage(DesktopContext context) {
        this.context = context;
        generatorSelector.getItems().setAll(generationOptions());
        generatorSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(GenerationOption option) {
                return option == null ? "" : Localization.text(option.label());
            }

            @Override
            public GenerationOption fromString(String value) {
                return generatorSelector.getItems().stream()
                        .filter(option -> Localization.text(option.label()).equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        generatorSelector.setMaxWidth(Double.MAX_VALUE);
        generatorSelector.setOnAction(event -> showSelectedGenerator());

        Label selectorLabel = new Label("Generation type:");
        HBox selector = new HBox(12, selectorLabel, generatorSelector);
        selector.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(generatorSelector, Priority.ALWAYS);

        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Generation",
                "Choose a generation type to create names, objects, concepts, and more."));
        root.getChildren().add(selector);
        root.getChildren().add(generatorContent);
        VBox.setVgrow(generatorContent, Priority.ALWAYS);

        if (!generatorSelector.getItems().isEmpty()) {
            generatorSelector.setValue(generatorSelector.getItems().getFirst());
            showSelectedGenerator();
        }
    }

    @Override
    public Node node() {
        return root;
    }

    private List<GenerationOption> generationOptions() {
        return List.of(
                new GenerationOption("Name generation", () -> new NameGenerationPanel().node()),
                new GenerationOption("Galaxy generation", () -> new GalaxyGenerationPanel(context).node()));
    }

    private void showSelectedGenerator() {
        GenerationOption option = generatorSelector.getValue();
        if (option != null) {
            Node selected = option.contentFactory().get();
            generatorContent.getChildren().setAll(selected);
            Localization.translate(selected);
        }
    }

    private record GenerationOption(String label, Supplier<Node> contentFactory) {
    }
}
