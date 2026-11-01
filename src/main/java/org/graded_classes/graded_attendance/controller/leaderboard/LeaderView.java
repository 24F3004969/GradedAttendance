package org.graded_classes.graded_attendance.controller.leaderboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

public class LeaderView implements Initializable {

    @FXML
    private Label classNo;

    @FXML
    private ImageView img;

    @FXML
    private Label level;

    @FXML
    private Label name;

    @FXML
    private Label no;

    @FXML
    private Label point;

    @FXML
    private Label titleOfStudents;
    StudentScore studentScore;
    int index;
    public LeaderView(StudentScore studentScore,int index) {
        this.studentScore = studentScore;
        this.index = index;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        no.setText(index+"");
        point.setText(String.valueOf(studentScore.points()));
        name.setText(studentScore.name());
        classNo.setText(studentScore.grade());
    }
}
