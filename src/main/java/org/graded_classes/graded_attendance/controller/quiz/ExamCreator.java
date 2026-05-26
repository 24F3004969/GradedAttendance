package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.SearchField;
import com.dlsc.gemsfx.TimePicker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.util.ResourceBundle;

public class ExamCreator implements Initializable {
    @FXML
    private ComboBox<String> classNum;

    @FXML
    private Button close;

    @FXML
    private TimePicker endTime;

    @FXML
    private DatePicker exam;

    @FXML
    private ComboBox<String> roomNo;

    @FXML
    private TimePicker startTime;

    @FXML
    private SearchField<String> subject;

    @FXML
    private SearchField<String> topicName;

    @FXML
    void create(ActionEvent event) {

    }

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
