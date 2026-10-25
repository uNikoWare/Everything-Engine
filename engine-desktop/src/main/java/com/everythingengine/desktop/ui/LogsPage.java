package com.everythingengine.desktop.ui;

import com.everythingengine.desktop.DesktopContext;
import com.everythingengine.desktop.diagnostics.DiagnosticEntry;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class LogsPage implements Page {
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private final DesktopContext context;
    private final VBox root = new VBox(14);
    private final TableView<DiagnosticEntry> table = new TableView<>();
    private final ObservableList<DiagnosticEntry> entries = FXCollections.observableArrayList();
    private final FilteredList<DiagnosticEntry> filtered = new FilteredList<>(entries);
    private final ComboBox<String> severity = new ComboBox<>();
    private final TextField search = new TextField();

    public LogsPage(DesktopContext context) {
        this.context = context;
        root.getStyleClass().add("page");
        root.getChildren().add(UiSupport.page(
                "Logs & Diagnostics", "Review application diagnostics captured through java.util.logging."));
        createTable();
        severity.getItems().setAll("All Levels", "DEBUG", "INFO", "WARN", "ERROR");
        severity.setValue("All Levels");
        severity.setOnAction(event -> applyFilter());
        search.setPromptText("Search source or message");
        search.textProperty().addListener((observable, before, after) -> applyFilter());
        ToolBar toolbar = new ToolBar(severity, search,
                UiSupport.button("Refresh", this::refresh));
        root.getChildren().add(toolbar);
        VBox panel = new VBox(8, new Label("Application events"), table);
        panel.getStyleClass().add("card");
        VBox.setVgrow(panel, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);
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
        TableColumn<DiagnosticEntry, String> time = UiSupport.column("Timestamp", 160);
        time.setCellValueFactory(cell -> new ReadOnlyStringWrapper(TIMESTAMP.format(cell.getValue().timestamp())));
        TableColumn<DiagnosticEntry, String> level = UiSupport.column("Level", 90);
        level.setCellValueFactory(cell -> new ReadOnlyStringWrapper(levelLabel(cell.getValue().level())));
        TableColumn<DiagnosticEntry, String> source = UiSupport.column("Source", 180);
        source.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().source()));
        TableColumn<DiagnosticEntry, String> message = UiSupport.column("Message", 500);
        message.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().message()));
        table.getColumns().setAll(List.of(time, level, source, message));
        table.setItems(filtered);
    }

    private void refresh() {
        entries.setAll(context.diagnostics().entries());
        applyFilter();
    }

    private void applyFilter() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        String level = severity.getValue();
        filtered.setPredicate(entry -> {
            boolean levelMatches = level == null || level.equals("All Levels")
                    || levelLabel(entry.level()).equals(level);
            boolean textMatches = query.isEmpty()
                    || entry.message().toLowerCase(Locale.ROOT).contains(query)
                    || entry.source().toLowerCase(Locale.ROOT).contains(query);
            return levelMatches && textMatches;
        });
    }

    private String levelLabel(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return "ERROR";
        }
        if (level.intValue() >= Level.WARNING.intValue()) {
            return "WARN";
        }
        if (level.intValue() >= Level.INFO.intValue()) {
            return "INFO";
        }
        return "DEBUG";
    }
}
