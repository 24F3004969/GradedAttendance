package org.graded_classes.graded_attendance.controller;

import atlantafx.base.controls.MaskTextField;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.data.Attendance;
import org.graded_classes.graded_attendance.data.Student;
import org.graded_classes.graded_attendance.report.AttendanceReport;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.graded_classes.graded_attendance.GradedResourceLoader.loadURL;

public class AttendanceDataView implements Initializable {
    @FXML
    public ToggleGroup optionsGroup;
    public RadioButton Submitted;
    public RadioButton NotSubmitted;
    @FXML
    private Label c_in_status;

    @FXML
    private Label c_out_status;

    @FXML
    private Label checkInTime;

    @FXML
    private Label checkOutTime;

    @FXML
    private Label dueAmount;
    @FXML
    private Tab edit, info;
    @FXML
    TabPane tabPane;
    @FXML
    Label todayTopic;

    @FXML
    private Label status;

    @FXML
    private Label uClass;

    @FXML
    private Label uId;
    @FXML
    private Label uName;
    StudentAttendance studentAttendance;
    String ed_no;
    EditStudentData editStudentData;

    public AttendanceDataView(StudentAttendance studentAttendance, String ed_no) {
        this.studentAttendance = studentAttendance;
        this.ed_no = ed_no;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        var x = studentAttendance.mainController.gradedDataLoader;
        Student student = x.getStudentData().get(ed_no);
        uId.setText(student.ed_no());
        uName.setText(firstLetterToUpperCase(student.name()));
        uClass.setText(student._class());
        makeAttendance();
        if (varify(studentAttendance.attendanceMap.get(ed_no))) {
            studentAttendance.checkIn_out.setVisible(true);
            if (studentAttendance.attendanceMap.get(ed_no).getCheck_in() == null)
                studentAttendance.checkIn_out.setText("Check In");
            else if (studentAttendance.attendanceMap.get(ed_no).getCheck_out() == null)
                studentAttendance.checkIn_out.setText("Check Out");
        } else {
            studentAttendance.checkIn_out.setVisible(false);
        }
        update();
        edit.setOnSelectionChanged(_ -> extracted());
    }

    private void makeAttendance() {
        if (!studentAttendance.attendanceMap.containsKey(ed_no)) {

            String sql = "INSERT INTO Attendance(ed_no,date) VALUES (?, ?)";
            String day = LocalDate.now().toString();

            try {
                Connection conn = studentAttendance.mainController.gradedDataLoader.databaseLoader.getConnection();
                PreparedStatement stat = conn.prepareStatement(sql);

                stat.setString(1, ed_no);
                stat.setString(2, day);
                stat.executeUpdate(); // <-- This is crucial

            } catch (SQLException e) {
                throw new RuntimeException("Database error: " + e.getMessage(), e);
            }

            studentAttendance.attendanceMap.put(ed_no, new Attendance(null, null, null, null));
        }
    }

    private void extracted() {
        var y = new FXMLLoader(loadURL("fxml/editData.fxml"));
        editStudentData = new EditStudentData(studentAttendance.mainController.gradedDataLoader.getStudentData(), ed_no);
        y.setControllerFactory(c -> editStudentData);
        try {
            edit.setContent(y.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean varify(Attendance s) {
        return s.getCheck_in() == null || s.getCheck_out() == null;
    }

    public static String firstLetterToUpperCase(String s) {
        String firstWord = s.substring(0, s.indexOf(' ')).trim();
        String secondWord = s.substring(s.indexOf(' ')).trim();
        return firstWord.substring(0, 1).toUpperCase() + firstWord.substring(1).toLowerCase() + " "
                + secondWord.substring(0, 1).toUpperCase() + secondWord.substring(1).toLowerCase();
    }

    public void update() {
        var x = studentAttendance.attendanceMap.get(ed_no);
        if (x != null) {
            if (x.getCheck_in() != null) {
                checkInTime.setText(x.getCheck_in());
                c_in_status.setText("Message sent");
                checkInTime.setStyle("-fx-text-fill: #1C75BC;");
                c_in_status.setStyle("-fx-text-fill: #1C75BC;");
            }
            if (x.getCheck_out() != null) {
                checkOutTime.setText(x.getCheck_out());
                c_out_status.setText("Message sent");
                checkOutTime.setStyle("-fx-text-fill: #1C75BC;");
                c_out_status.setStyle("-fx-text-fill: #1C75BC;");

            }
            if (x.getCheck_in() != null && x.getCheck_out() != null) {
                status.setText("Present");
                status.setStyle("-fx-text-fill: #1C75BC;");
            }
            if (x.getHomework_status() != null && x.getHomework_status()) {
                Submitted.setSelected(true);
            }
            if (x.getHomework_status() != null && !x.getHomework_status()) {
                NotSubmitted.setSelected(true);
            }
            todayTopic.setText(x.getTopics());
        }

    }

    @FXML
    void onCancel() {
        studentAttendance.outer_main_box.getChildren().removeLast();
        studentAttendance.inputField.setText("");
        studentAttendance.searchCrossIcon.setImage(new Image(GradedResourceLoader.load("icons/search.svg")));

    }


    @FXML
    void viewReport(ActionEvent event) {
        AttendanceReport attendanceReport = new AttendanceReport(studentAttendance.mainController.gradedDataLoader.databaseLoader);
        attendanceReport.init();
        var x = studentAttendance.mainController.gradedDataLoader;
        Student student = x.getStudentData().get(ed_no);
        var view = attendanceReport.getStudentAttendanceReport();
        studentAttendance.mainController.
                modalPane.show(studentAttendance.mainController.
                        gradedFxmlLoader.createView(R.attendance_report,
                                new AttendanceReportController(firstLetterToUpperCase(student.name()), ed_no, view)));
    }
    @FXML
    void onSave() {
      if (edit.isSelected()) {
            editStudentData.update(studentAttendance.mainController.gradedDataLoader.databaseLoader.getConnection());
        }
    }


    @FXML
    void editTime(MouseEvent event) {
        Label time = (Label) event.getSource();
        System.out.println(time.getId());
        if (time.getText().equals("Unknown")) {
            Node node = studentAttendance.mainController.gradedFxmlLoader.createView(R.edit_time);
            MaskTextField timeField = (MaskTextField) (node.lookup("#textfield"));
            timeField.setMask("19:59" + " " + (LocalTime.now().getHour() < 12 ? "am" : "pm"));
            studentAttendance.mainController.modalPane.show(node);
            var timeFormatter = DateTimeFormatter.ofPattern("hh:mm");
            timeField.setText(
                    LocalTime.now(ZoneId.systemDefault()).format(timeFormatter)
            );
            timeField.setLeft(new FontIcon(Material2OutlinedMZ.TIMER));
            timeField.setPrefWidth(120);
            timeField.textProperty().addListener((obs, old, val) -> {
                if (val != null) {
                    try {
                        LocalTime.parse(val.toUpperCase(), DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
                        timeField.pseudoClassStateChanged(Styles.STATE_DANGER, false);
                        if (time.getId().equals("onLabelIn")) {
                            studentAttendance.updateAttendance(new Button("Check In"), false, val);
                            update();
                        } else if (time.getId().equals("onLabelOut")) {
                            studentAttendance.updateAttendance(new Button("Check Out"), false, val);
                            update();
                        }

                    } catch (DateTimeParseException e) {
                        System.out.println(e.getMessage());
                        timeField.pseudoClassStateChanged(Styles.STATE_DANGER, true);
                    }
                }
            });
        }
    }

    public void addTopic(MouseEvent mouseEvent) {
        TodayTopic todayTopic = new TodayTopic(this);
        studentAttendance.
                mainController.
                modalPane.show(studentAttendance.
                        mainController.
                        gradedFxmlLoader.
                        createView(R.topic_taught_today, todayTopic));


    }
}
