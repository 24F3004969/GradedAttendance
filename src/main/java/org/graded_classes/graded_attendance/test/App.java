package org.graded_classes.graded_attendance.test;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.data.FeeData;

import java.util.List;

public class App extends Application {

    private TableView<FeeData> table = new TableView<>();
    private ProgressIndicator spinner = new ProgressIndicator();

    @Override
    public void start(Stage stage) {
        setupColumns();

        VBox root = new VBox(10, table, spinner);
        root.setPadding(new Insets(12));
        stage.setScene(new Scene(root, 800, 600));
        stage.show();

        loadData();
    }

    private void loadData() {
        Task<ObservableList<FeeData>> task = new Task<>() {
            @Override
            protected ObservableList<FeeData> call() throws Exception {
                // Simulate DB fetch
                List<FeeData> rows = inits(); // your DB query
                return FXCollections.observableArrayList(rows);
            }
        };

        spinner.visibleProperty().bind(task.runningProperty());
        table.disableProperty().bind(task.runningProperty());

        task.setOnSucceeded(e -> table.setItems(task.getValue()));
        task.setOnFailed(e -> showError("DB load failed", task.getException()));

        new Thread(task) {{
            setDaemon(true);
        }}.start();
    }

    private void setupColumns() {
        // define columns and cell value factories
    }

    public List<FeeData> inits() {
        // your DB call (blocking OK here because it's in background Task)
        // return fetched rows
        return List.of();
    }

    private void showError(String message, Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message + "\n" + ex.getMessage());
        alert.showAndWait();
    }
}
