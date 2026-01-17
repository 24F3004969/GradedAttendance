package org.graded_classes.graded_attendance.test;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.components.LatexView;

public class LatexTest extends Application {

    @FXML
    private LatexView latexView;

    @FXML
    private TextArea textArea;

    @FXML
    private void update(ActionEvent event) {
        latexView.setFormula(textArea.getText());
    }

    public static void main(String... args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader root = new FXMLLoader(GradedResourceLoader.loadURL("fxml/example.fxml"));
        Scene scene = new Scene(root.load(), 640, 480);
        stage.setScene(scene);
        stage.setTitle("Latex Test");
        stage.show();
    }
}