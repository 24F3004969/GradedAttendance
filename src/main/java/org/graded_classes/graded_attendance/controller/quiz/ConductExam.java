package org.graded_classes.graded_attendance.controller.quiz;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.util.ResourceBundle;

public class ConductExam implements Initializable {
    MainController mainController;
    private TableColumn<?, ?> action;

    @FXML
    private TableColumn<?, ?> classes;

    @FXML
    private TableColumn<?, ?> doe;

    @FXML
    private TableColumn<?, ?> id;

    @FXML
    private TableColumn<?, ?> room;

    @FXML
    private TableView<?> scheduleTable;

    @FXML
    private TableColumn<?, ?> subject;

    @FXML
    private TableColumn<?, ?> time;

    @FXML
    private Label today;

    @FXML
    private TableColumn<?, ?> topic;
    public ConductExam(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void addNewExam(ActionEvent event) {
        mainController.modalPane.setPersistent(true);
        mainController.modalPane.show(mainController.gradedFxmlLoader.createView(R.exam_create,new ExamCreator(mainController)));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
