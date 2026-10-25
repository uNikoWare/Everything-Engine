package com.everythingengine.desktop.ui;

import com.everythingengine.core.distribution.DiskDistribution;
import com.everythingengine.core.distribution.PositionDistribution;
import com.everythingengine.core.distribution.RadialDistribution;
import com.everythingengine.core.distribution.SpiralDistribution;
import com.everythingengine.core.distribution.UniformEllipsoidDistribution;
import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.galaxy.GalaxyType;
import com.everythingengine.core.galaxy.generation.GalaxyGenerationConfig;
import com.everythingengine.core.galaxy.generation.GalaxyGenerationMode;
import com.everythingengine.core.galaxy.generation.GalaxyGenerationResult;
import com.everythingengine.core.galaxy.generation.GalaxyGenerator;
import com.everythingengine.core.galaxy.structure.SpiralArmGeometry;
import com.everythingengine.core.spatial.CoordinateSpace;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.desktop.DesktopContext;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class GalaxyGenerationPanel {
    private final DesktopContext context;
    private final GalaxyGenerator generator = new GalaxyGenerator();
    private final TextField name = new TextField();
    private final NumericValueControl radius = new NumericValueControl("50000",
            NumericValueControl.Kind.DECIMAL);
    private final NumericValueControl population = new NumericValueControl("1000",
            NumericValueControl.Kind.INTEGER);
    private final NumericValueControl seed = new NumericValueControl("42",
            NumericValueControl.Kind.LONG);
    private final ComboBox<GenerationChoice<GalaxyType>> type = new ComboBox<>();
    private final ComboBox<GenerationChoice<GalaxyGenerationMode>> mode = new ComboBox<>();
    private final ComboBox<GenerationChoice<DistributionKind>> distribution = new ComboBox<>();
    private final ComboBox<GenerationChoice<Boolean>> createStars = new ComboBox<>();
    private final Button generate = UiSupport.button("Generate galaxy", this::startGeneration);
    private final Label status = new Label("Configure a galaxy and start generation.");
    private final TableView<Summary> results = new TableView<>();
    private final VBox root = new VBox(14);

    public GalaxyGenerationPanel(DesktopContext context) {
        this.context = context;
        type.getItems().setAll(GenerationChoice.random());
        type.getItems().addAll(List.of(GalaxyType.SPIRAL, GalaxyType.BARRED_SPIRAL,
                        GalaxyType.ELLIPTICAL, GalaxyType.LENTICULAR, GalaxyType.IRREGULAR).stream()
                .map(galaxyType -> GenerationChoice.fixed(galaxyType.key(), galaxyType))
                .toList());
        type.setValue(type.getItems().get(1));
        type.setConverter(choiceConverter(type));
        mode.getItems().setAll(GenerationChoice.random());
        mode.getItems().addAll(List.of(GalaxyGenerationMode.values()).stream()
                .map(value -> GenerationChoice.fixed(value.name(), value))
                .toList());
        mode.setValue(mode.getItems().get(1));
        mode.setConverter(choiceConverter(mode));
        distribution.getItems().setAll(GenerationChoice.random());
        distribution.getItems().addAll(List.of(DistributionKind.values()).stream()
                .map(value -> GenerationChoice.fixed(value.label(), value))
                .toList());
        distribution.setValue(distribution.getItems().get(2));
        distribution.setConverter(choiceConverter(distribution));
        createStars.getItems().setAll(GenerationChoice.random(),
                GenerationChoice.fixed("Yes", true), GenerationChoice.fixed("No", false));
        createStars.setValue(createStars.getItems().get(1));
        createStars.setConverter(choiceConverter(createStars));
        mode.setOnAction(event -> updateModeControls());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Galaxy name (blank = generate):"), name);
        form.addRow(1, new Label("Galaxy type:"), type);
        form.addRow(2, new Label("Radius (parsecs):"), radius);
        form.addRow(3, new Label("Generation mode:"), mode);
        form.addRow(4, new Label("System count:"), population);
        form.addRow(5, new Label("Spatial distribution:"), distribution);
        form.addRow(6, new Label("Seed:"), seed);
        form.addRow(7, new Label("Create one sample star per system:"), createStars);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(type, Priority.ALWAYS);
        GridPane.setHgrow(mode, Priority.ALWAYS);
        GridPane.setHgrow(population, Priority.ALWAYS);
        GridPane.setHgrow(distribution, Priority.ALWAYS);
        GridPane.setHgrow(radius, Priority.ALWAYS);
        GridPane.setHgrow(seed, Priority.ALWAYS);
        form.setMaxWidth(680);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));

        generate.getStyleClass().add("primary-button");
        status.setWrapText(true);
        status.getStyleClass().add("muted");
        configureResultsTable();
        VBox.setVgrow(results, Priority.ALWAYS);
        HBox actions = new HBox(12, generate, status);
        actions.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(form, actions, results);
        updateModeControls();
    }

    public Node node() {
        return root;
    }

    private void startGeneration() {
        GalaxyGenerationConfig config;
        try {
            double radiusParsecs = radius.sample().doubleValue();
            GalaxyGenerationMode selectedMode = selected(mode);
            int systemCount = selectedMode == GalaxyGenerationMode.STRUCTURAL
                    ? 0 : population.sample().intValue();
            if (selectedMode != GalaxyGenerationMode.STRUCTURAL
                    && (systemCount < 1 || systemCount > 10_000)) {
                throw new IllegalArgumentException(
                        Localization.text("System count must be between 1 and 10000."));
            }
            long randomSeed = seed.sample().longValue();
            GalaxyExtent extent = GalaxyExtent.spherical(radiusParsecs);
            config = new GalaxyGenerationConfig(name.getText(), selected(type),
                    new Position3D(new CoordinateSpace("generated-galaxy"), 0, 0, 0), extent,
                    selectedMode, systemCount, distributionFor(extent), Optional.empty(),
                    selected(createStars), randomSeed);
        } catch (IllegalArgumentException exception) {
            status.setText(Localization.text("Invalid configuration: ") + exception.getMessage());
            return;
        }

        generate.setDisable(true);
        status.setText(Localization.text("Generating galaxy..."));
        Task<GalaxyGenerationResult> task = new Task<>() {
            @Override
            protected GalaxyGenerationResult call() {
                return generator.generate(config);
            }
        };
        task.setOnSucceeded(event -> {
            generate.setDisable(false);
            GalaxyGenerationResult result = task.getValue();
            if (!result.isValid()) {
                status.setText(Localization.text("Generation failed: ")
                        + result.validation().errors().getFirst().message());
                return;
            }
            try {
                result.register(context.universe());
            } catch (RuntimeException exception) {
                status.setText(Localization.text("Could not register generated galaxy: ")
                        + exception.getMessage());
                return;
            }
            showResult(result);
            try {
                context.saveUniverse(context.universeFile());
                status.setText(Localization.text("Galaxy generated and registered."));
            } catch (IOException exception) {
                status.setText(Localization.text("Galaxy registered, but automatic saving failed: ")
                        + exception.getMessage());
            }
        });
        task.setOnFailed(event -> {
            generate.setDisable(false);
            status.setText(Localization.text("Generation failed: ") + task.getException().getMessage());
        });
        Thread worker = new Thread(task, "galaxy-generator");
        worker.setDaemon(true);
        worker.start();
    }

    private Optional<PositionDistribution> distributionFor(GalaxyExtent extent) {
        return Optional.of(switch (selected(distribution)) {
            case UNIFORM -> new UniformEllipsoidDistribution(extent);
            case DISK -> new DiskDistribution(extent.radiusXParsecs() * 0.8, extent.radiusZParsecs() * 0.1);
            case RADIAL -> new RadialDistribution(extent.radiusXParsecs() * 0.9, 2);
            case SPIRAL -> new SpiralDistribution(new SpiralArmGeometry(
                    extent.radiusXParsecs() * 0.1, Math.toRadians(12), 0, 0.4,
                    extent.radiusZParsecs() * 0.05, extent.radiusZParsecs() * 0.02));
        });
    }

    private void updateModeControls() {
        boolean structural = mode.getValue() != null
                && mode.getValue().value().filter(value -> value == GalaxyGenerationMode.STRUCTURAL).isPresent();
        population.setDisable(structural);
        distribution.setDisable(structural);
        createStars.setDisable(structural);
    }

    private void configureResultsTable() {
        TableColumn<Summary, String> property = new TableColumn<>(Localization.text("Property"));
        property.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().property()));
        TableColumn<Summary, String> value = new TableColumn<>(Localization.text("Value"));
        value.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().value()));
        results.getColumns().add(property);
        results.getColumns().add(value);
        property.setPrefWidth(220);
        value.setPrefWidth(360);
        results.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        results.setPlaceholder(new Label("Generated galaxy details will appear here."));
        results.setPrefHeight(240);
    }

    private void showResult(GalaxyGenerationResult result) {
        var galaxy = result.galaxy().orElseThrow();
        results.getItems().setAll(List.of(
                new Summary("Galaxy", galaxy.name()),
                new Summary("Type", galaxy.type().key()),
                new Summary("Extent (parsecs)", radiusValue(galaxy.extent())),
                new Summary("Structures", Integer.toString(result.structures().size())),
                new Summary("Stellar systems", Integer.toString(result.systems().size())),
                new Summary("Stellar objects", Integer.toString(result.stars().size())),
                new Summary("Seed", Long.toString(result.seed())),
                new Summary("Validation", Localization.text("Valid"))));
    }

    private String radiusValue(GalaxyExtent extent) {
        return extent.radiusXParsecs() + " × " + extent.radiusYParsecs() + " × " + extent.radiusZParsecs();
    }

    private <T> StringConverter<GenerationChoice<T>> choiceConverter(
            ComboBox<GenerationChoice<T>> selector) {
        return new StringConverter<>() {
            @Override
            public String toString(GenerationChoice<T> choice) {
                return choice == null ? "" : Localization.text(choice.label());
            }

            @Override
            public GenerationChoice<T> fromString(String value) {
                return selector.getItems().stream()
                        .filter(choice -> Localization.text(choice.label()).equals(value))
                        .findFirst().orElse(null);
            }
        };
    }

    private <T> T selected(ComboBox<GenerationChoice<T>> selector) {
        GenerationChoice<T> choice = selector.getValue();
        if (choice == null) {
            throw new IllegalArgumentException(Localization.text("Select a value or Random."));
        }
        return choice.value().orElseGet(() -> {
            List<T> values = selector.getItems().stream()
                    .flatMap(item -> item.value().stream())
                    .toList();
            if (values.isEmpty()) {
                throw new IllegalArgumentException(Localization.text("No random choices are available."));
            }
            return values.get(ThreadLocalRandom.current().nextInt(values.size()));
        });
    }

    private record Summary(String property, String value) {
    }

    private enum DistributionKind {
        UNIFORM("Uniform volume"),
        DISK("Disk"),
        RADIAL("Radial"),
        SPIRAL("Spiral arm");

        private final String label;

        DistributionKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
