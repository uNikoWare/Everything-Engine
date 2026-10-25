package com.everythingengine.desktop.ui;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.relation.Relation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.time.Duration;

public final class UiSupport {
    private UiSupport() {
    }

    public static VBox page(String title, String subtitle) {
        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label description = new Label(subtitle);
        description.getStyleClass().add("page-subtitle");
        VBox header = new VBox(5, heading, description);
        header.getStyleClass().add("page-header");
        return header;
    }

    public static Button button(String label, Runnable action) {
        Button button = new Button(label);
        button.setOnAction(event -> action.run());
        return button;
    }

    public static void showError(String title, RuntimeException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage());
        alert.showAndWait();
    }

    public static String entityType(Entity entity) {
        return entity.getClass().getSimpleName();
    }

    public static String entityLabel(Entity entity) {
        return entity.name() + " (" + entity.getClass().getSimpleName() + ")";
    }

    public static String endpoint(Relation relation, boolean source) {
        Entity entity = source ? relation.source() : relation.target();
        return entity.name() + " · " + entity.id();
    }

    public static String duration(Duration duration) {
        return String.format("%d.%03d s", duration.toSeconds(), duration.toMillisPart());
    }

    public static <T> void constrained(TableView<T> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No data available."));
    }

    public static <S, T> TableColumn<S, T> column(String title, double width) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        return column;
    }
}
