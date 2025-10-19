package org.graded_classes.graded_attendance.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.R;

import java.net.URL;
import java.util.ResourceBundle;

public class ListViewStudents implements Initializable {
    @FXML
    private ListView<HBox> list;
    StudentAttendance studentAttendance;
    String ed;
    AttendanceDataView attendanceDataView;
    String name;
    public ListViewStudents(StudentAttendance studentAttendance) {
        this.studentAttendance = studentAttendance;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        list.setItems(studentAttendance.filteredData);
        list.setMinWidth(300);
        list.getSelectionModel().selectedItemProperty().
                addListener((observable,
                             oldValue, newValue) ->
                {
                    if (newValue != null) {
                        String id = newValue.getId();
                        ed = id.substring(0, id.indexOf(" "));
                        name = id.substring(id.indexOf(" ") + 1);
                        studentAttendance.inputField.setText(name);
                        studentAttendance.searchCrossIcon.setImage(new Image(GradedResourceLoader.load("icons/close.svg")));
                        createCustomDataView(ed);
                        studentAttendance.hide_search();
                    }
                });

    }

    private void createCustomDataView(String ed) {
        switch (studentAttendance.id) {
            case "stu_atten" -> {
                attendanceDataView = new AttendanceDataView(this.studentAttendance, ed);
                var attendance = studentAttendance.gradedFxmlLoader.createView(R.data_view, attendanceDataView);
                if (studentAttendance.outer_main_box.getChildren().size() > 3)
                    studentAttendance.outer_main_box.getChildren().set(studentAttendance.outer_main_box.getChildren().size() - 1, attendance);
                else
                    studentAttendance.outer_main_box.getChildren().add(attendance);
                VBox.setVgrow(attendance, Priority.ALWAYS);
            }
            case "st_fee" -> {
                var feeReport = studentAttendance.gradedFxmlLoader.createView(R.student_fee_layout,
                        new StudentFeeLayout(studentAttendance.mainController, ed, name));
                if (studentAttendance.outer_main_box.getChildren().size() > 3)
                    studentAttendance.outer_main_box.getChildren().set(studentAttendance.outer_main_box.getChildren().size() - 1, feeReport);
                else
                    studentAttendance.outer_main_box.getChildren().add(feeReport);
            }
            case "tea_prog" -> {
                var teaching = studentAttendance.gradedFxmlLoader.createView(R.teaching_progress_report);
                if (studentAttendance.outer_main_box.getChildren().size() > 3)
                    studentAttendance.outer_main_box.getChildren().set(studentAttendance.outer_main_box.getChildren().size() - 1, teaching);
                else
                    studentAttendance.outer_main_box.getChildren().add(teaching);
            }
        }
    }
}
