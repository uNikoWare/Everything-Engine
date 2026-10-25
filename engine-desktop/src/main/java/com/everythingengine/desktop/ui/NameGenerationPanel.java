package com.everythingengine.desktop.ui;

import com.everythingengine.core.name.NameGenerator;
import com.everythingengine.core.name.NameProfile;
import com.everythingengine.core.name.NameProfiles;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class NameGenerationPanel {
    private final NameGenerator generator = new NameGenerator(202406L);
    private final ComboBox<GenerationChoice<NameProfile>> profileSelector = new ComboBox<>();
    private final NumericValueControl nameCount = new NumericValueControl("8", NumericValueControl.Kind.INTEGER);
    private final ListView<String> generatedNames = new ListView<>();
    private final Label profileSummary = new Label();
    private final Label generationSummary = new Label();
    private final VBox root = new VBox(16);

    public NameGenerationPanel() {
        generator.registerProfile(NameProfiles.exoticSyllableProfile());
        generator.registerProfile(NameProfiles.compoundLocationProfile());
        generator.registerProfile(NameProfiles.technicalTemplateProfile());

        profileSelector.getItems().setAll(GenerationChoice.random());
        profileSelector.getItems().addAll(generator.profiles().stream()
                .map(profile -> GenerationChoice.fixed(profile.name(), profile))
                .toList());
        if (!generator.profiles().isEmpty()) {
            profileSelector.setValue(profileSelector.getItems().get(1));
        }
        profileSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(GenerationChoice<NameProfile> choice) {
                return choice == null ? "" : Localization.text(choice.label());
            }

            @Override
            public GenerationChoice<NameProfile> fromString(String value) {
                return profileSelector.getItems().stream()
                        .filter(choice -> Localization.text(choice.label()).equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        profileSelector.setOnAction(event -> refreshProfileDetails());

        Button generateButton = UiSupport.button("Generate sample set", this::generateSampleSet);
        generateButton.getStyleClass().add("primary-button");

        HBox controls = new HBox(12, new Label("Profile:"), profileSelector,
                new Label("Number of names:"), nameCount, generateButton);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(profileSelector, Priority.ALWAYS);

        profileSummary.setWrapText(true);
        profileSummary.getStyleClass().add("muted");
        generationSummary.getStyleClass().add("section-title");
        generatedNames.setPrefHeight(260);

        root.getChildren().add(new VBox(8,
                controls,
                profileSummary,
                generationSummary,
                generatedNames));

        refreshProfileDetails();
        generateSampleSet();
    }

    public Node node() {
        return root;
    }

    private void refreshProfileDetails() {
        GenerationChoice<NameProfile> selection = profileSelector.getValue();
        if (selection == null) {
            profileSummary.setText("No profile selected.");
            generationSummary.setText("No names generated yet.");
            generatedNames.getItems().clear();
            return;
        }
        if (selection.value().isEmpty()) {
            profileSummary.setText(Localization.text("A random profile will be selected."));
            generationSummary.setText(Localization.text("Random profile selected."));
            return;
        }
        NameProfile profile = selection.value().orElseThrow();
        profileSummary.setText(Localization.text(profile.description())
                + "\n" + Localization.text("Strategy: ") + Localization.text(profile.strategy().name())
                + "\n" + Localization.text("Maximum attempts: ") + profile.maxAttempts());
        generationSummary.setText(Localization.text("Selected profile: ") + Localization.text(profile.name()));
    }

    private void generateSampleSet() {
        GenerationChoice<NameProfile> selection = profileSelector.getValue();
        if (selection == null) {
            return;
        }
        NameProfile profile = selection.value().orElseGet(() -> generator.profiles().get(
                ThreadLocalRandom.current().nextInt(generator.profiles().size())));
        int count;
        try {
            count = nameCount.sample().intValue();
            if (count < 1 || count > 10_000) {
                throw new IllegalArgumentException(Localization.text("Number of names must be between 1 and 10000."));
            }
        } catch (IllegalArgumentException exception) {
            generationSummary.setText(Localization.text("Invalid configuration: ") + exception.getMessage());
            return;
        }
        List<String> names = generator.generateMany(profile, count, true);
        generatedNames.getItems().setAll(names);
        generationSummary.setText(Localization.text("Generated ") + count + " "
                + Localization.text("names for: ") + Localization.text(profile.name()));
    }
}
