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

import java.util.List;

public final class NameGenerationPage implements Page {
    private final NameGenerator generator = new NameGenerator(202406L);
    private final ComboBox<NameProfile> profileSelector = new ComboBox<>();
    private final ListView<String> generatedNames = new ListView<>();
    private final Label profileSummary = new Label();
    private final Label generationSummary = new Label();
    private final VBox root = new VBox(16);

    public NameGenerationPage() {
        generator.registerProfile(NameProfiles.exoticSyllableProfile());
        generator.registerProfile(NameProfiles.compoundLocationProfile());
        generator.registerProfile(NameProfiles.technicalTemplateProfile());

        profileSelector.getItems().setAll(generator.profiles());
        if (!generator.profiles().isEmpty()) {
            profileSelector.setValue(generator.profiles().getFirst());
        }
        profileSelector.setOnAction(event -> refreshProfileDetails());

        Button generateButton = UiSupport.button("Generate sample set", this::generateSampleSet);
        generateButton.getStyleClass().add("primary-button");

        HBox controls = new HBox(12, new Label("Profile:"), profileSelector, generateButton);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(profileSelector, Priority.ALWAYS);

        profileSummary.setWrapText(true);
        profileSummary.getStyleClass().add("muted");
        generationSummary.getStyleClass().add("section-title");
        generatedNames.setPrefHeight(260);

        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Name Generation",
                "Preview deterministic generated names for the core naming module."));
        root.getChildren().add(new VBox(8,
                controls,
                profileSummary,
                generationSummary,
                generatedNames));

        refreshProfileDetails();
        generateSampleSet();
    }

    @Override
    public Node node() {
        return root;
    }

    private void refreshProfileDetails() {
        NameProfile profile = profileSelector.getValue();
        if (profile == null) {
            profileSummary.setText("No profile selected.");
            generationSummary.setText("No names generated yet.");
            generatedNames.getItems().clear();
            return;
        }
        profileSummary.setText(profile.description()
                + "\nStrategy: " + profile.strategy()
                + "\nMaximum attempts: " + profile.maxAttempts());
        generationSummary.setText("Selected profile: " + profile.name());
    }

    private void generateSampleSet() {
        NameProfile profile = profileSelector.getValue();
        if (profile == null) {
            return;
        }
        List<String> names = generator.generateMany(profile, 8, true);
        generatedNames.getItems().setAll(names);
        generationSummary.setText("Generated 8 names for: " + profile.name());
    }
}
