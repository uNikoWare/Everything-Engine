package com.everythingengine.desktop.ui;

import com.everythingengine.desktop.DesktopContext;
import com.everythingengine.desktop.testing.MavenTestService;
import com.everythingengine.desktop.testing.TestRunResult;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class TestingPage implements Page {
    private final DesktopContext context;
    private final VBox root = new VBox(15);
    private final Button runButton = new Button("Run Maven Tests");
    private final Label result = new Label("Not Run");
    private final Label totals = new Label("Test results will appear here.");
    private final Label prerequisites = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final TextArea output = new TextArea();
    private MavenTestService service;

    public TestingPage(DesktopContext context) {
        this.context = context;
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Testing Center", "Run the project's fixed Maven test goal without blocking the interface."));
        prerequisites.getStyleClass().add("muted");
        prerequisites.setText(context.projectRoot() == null
                ? "Unavailable: no pom.xml was found from the current working directory."
                : "Project: " + context.projectRoot() + " · Command: mvn -B test");
        runButton.getStyleClass().add("primary-button");
        runButton.setDisable(context.projectRoot() == null);
        runButton.setOnAction(event -> runTests());
        progress.setPrefSize(24, 24);
        progress.setVisible(false);
        HBox controls = new HBox(12, runButton, progress, result);
        controls.setPadding(new Insets(4, 0, 4, 0));
        result.getStyleClass().add("section-title");
        VBox report = new VBox(8, new Label("Latest run"), totals, output);
        report.getStyleClass().add("card");
        VBox.setVgrow(output, Priority.ALWAYS);
        VBox.setVgrow(report, Priority.ALWAYS);
        output.setEditable(false);
        output.setWrapText(false);
        output.setPromptText("No test run has been performed.");
        root.getChildren().addAll(prerequisites, controls, report);
        if (context.lastTestResult() != null) {
            showResult(context.lastTestResult());
        }
    }

    @Override
    public Node node() {
        return root;
    }

    private void runTests() {
        if (service != null && service.isRunning()) {
            return;
        }
        service = new MavenTestService(context.projectRoot());
        service.setOnRunning(event -> {
            runButton.setDisable(true);
            progress.setVisible(true);
            result.setText("Running");
            totals.setText("Maven test process is running in the background.");
            context.diagnostics().info("Testing", "Started Maven test run.");
        });
        service.setOnSucceeded(event -> {
            progress.setVisible(false);
            runButton.setDisable(false);
            context.setLastTestResult(service.getValue());
            showResult(service.getValue());
            context.diagnostics().info("Testing", service.getValue().message());
        });
        service.setOnFailed(event -> {
            progress.setVisible(false);
            runButton.setDisable(false);
            Throwable error = service.getException();
            String message = error == null ? "Test process failed." : error.getMessage();
            result.setText("Error");
            totals.setText(message);
            output.setText(message == null ? "Unknown test runner error." : message);
            context.setLastTestResult(new TestRunResult(
                    TestRunResult.Status.ERROR, 0, 0, 0, java.time.Duration.ZERO, java.time.Instant.now(),
                    message == null ? "Unknown test runner error." : message, output.getText()));
            context.diagnostics().error("Testing", "Maven test run failed.", error);
        });
        service.start();
    }

    private void showResult(TestRunResult run) {
        result.setText(run.status().name());
        totals.setText(run.passed() + " passed · " + run.failed() + " failed · "
                + run.skipped() + " skipped · " + UiSupport.duration(run.duration())
                + "\n" + run.message());
        output.setText(run.output());
        if (run.output().isBlank()) {
            output.setText("No process output.");
        }
    }
}
