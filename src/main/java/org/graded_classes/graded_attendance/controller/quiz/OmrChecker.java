package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.util.ResourceBundle;

public class OmrChecker implements Initializable {
    @FXML
    private Button close;

    @FXML
    private SplitPane omrView;

    @FXML
    private ToggleGroup typeType;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
