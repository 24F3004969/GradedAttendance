package org.graded_classes.graded_attendance.controller;

import atlantafx.base.theme.Styles;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class TodayTopic implements Initializable {
    AttendanceDataView studentAttendanceView;
    @FXML
    private TextField subject1;

    @FXML
    private TextField subject2;

    @FXML
    private TextField topic1;

    @FXML
    private TextField topic2;

    @FXML
    void addMyTopic() {
        if (subject1.getText().isEmpty() || topic1.getText().isEmpty()) {
            subject1.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            topic1.pseudoClassStateChanged(Styles.STATE_DANGER, true);

        } else {
            studentAttendanceView.studentAttendance.topicTaughtTodayUpdate(subject1.getText(),
                    topic1.getText(), subject2.getText(), topic2.getText());
            studentAttendanceView.todayTopic.setText(subject1.getText() + ":"
                    + topic1.getText() + "," + subject2.getText() + ":" + topic2.getText());
            studentAttendanceView.studentAttendance.mainController.modalPane.hide();
        }
    }

    public TodayTopic(AttendanceDataView studentAttendanceView) {
        this.studentAttendanceView = studentAttendanceView;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
