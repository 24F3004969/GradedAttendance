package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.util.ResourceBundle;

public class ExamCreator implements Initializable {
    @FXML
    private Button close;
    MainController mainController;
    public ExamCreator(MainController mainController) {
        this.mainController = mainController;
    }
    @FXML
    void create() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
            close.setOnMouseClicked(event -> {
               this.mainController.modalPane.hide();
            });
    }
}
