package com.everythingengine.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Objects;

public final class EverythingEngineApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            DesktopContext context = new DesktopContext();
            MainWindow window = new MainWindow(context);
            Scene scene = new Scene(window.root(), 1280, 820);
            var stylesheet = getClass().getResource("/styles/app.css");
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }
            window.applyTheme(scene);
            stage.setTitle("Everything Engine");
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.setScene(scene);
            stage.show();
            Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                context.diagnostics().error("Uncaught", "Unexpected error on " + thread.getName(), exception);
            });
        } catch (RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("Everything Engine could not start.");
            alert.setContentText(Objects.toString(exception.getMessage(), exception.getClass().getSimpleName()));
            alert.showAndWait();
            throw exception;
        }
    }
}
