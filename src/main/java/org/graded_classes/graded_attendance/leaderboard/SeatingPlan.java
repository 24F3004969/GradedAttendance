package org.graded_classes.graded_attendance.leaderboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class SeatingPlan implements Initializable {

    @FXML
    private VBox grade;

    @FXML
    private VBox room_no;

    @FXML
    private VBox subject;

    @FXML
    private HBox teacher;

    @FXML
    private VBox timing;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}