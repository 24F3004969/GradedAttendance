package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.SegmentedControl;
import atlantafx.base.controls.ToggleLabel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class QuestionEditor implements Initializable {

    ArrayList<VBox> listOfQuestions = new ArrayList<>();
    @FXML
    private ScrollPane question_scroll;
    @FXML
    MainController mainController;

    public QuestionEditor(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private ScrollPane editor;

    @FXML
    private SegmentedControl segmentControl;

    @FXML
    private Label status;

    @FXML
    void onBasicAction(ActionEvent event) {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        segmentControl.getSegments().add(new ToggleLabel("Edit"));
        segmentControl.getSegments().add(new ToggleLabel("Preview"));
        editor.setContent(mainController.gradedFxmlLoader.createView(R.question, new Questions(mainController)));

    }

}