package org.graded_classes.graded_attendance.controller;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.GradedFxmlLoader;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.data.Attendance;
import org.graded_classes.graded_attendance.data.Student;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.graded_classes.graded_attendance.GradedResourceLoader.loadURL;

public class StudentAttendance implements Initializable {
    @FXML
    public ImageView searchCrossIcon;
    @FXML
    TextField inputField;
    @FXML
    ComboBox<String> choiceBox;
    VBox box;
    ObservableList<HBox> observableList;
    FilteredList<HBox> filteredData;
    @FXML
    private VBox search_box;
    @FXML
    Button checkIn_out;
    ListView<HBox> list;
    MainController mainController;
    GradedFxmlLoader gradedFxmlLoader;
    VBox outer_main_box;
    String id;
    ListViewStudents listViewStudents;
    LinkedHashMap<String, Attendance> attendanceMap = new LinkedHashMap<>();

    public ArrayList<HBox> getBoxes() {
        return boxes;
    }

    ArrayList<HBox> boxes = new ArrayList<>();

    public StudentAttendance(MainController mainController,
                             GradedFxmlLoader gradedFxmlLoader,
                             VBox outer_main_box, String id) {
        this.mainController = mainController;
        this.gradedFxmlLoader = gradedFxmlLoader;
        this.outer_main_box = outer_main_box;
        this.id = id;
        init();
    }

    private void init() {
        String date = LocalDate.now().toString();
        try {
            var stmt = mainController.gradedDataLoader.databaseLoader.getConnection();
            String sql = "SELECT * FROM Attendance WHERE date = ?";
            PreparedStatement pst = stmt.prepareStatement(sql);
            pst.setString(1, date);
            ResultSet r = pst.executeQuery();
            while (r.next()) {

                attendanceMap.put(r.getString("ed_no"),
                        new Attendance(r.getString("check_in"),
                                r.getString("check_out"),
                                r.getString("homework")));
            }
            System.out.println(attendanceMap);

        } catch (SQLException _) {

        }
    }

    @FXML
    void hide_search() {

        if (search_box.getChildren().size() == 2) {
            search_box.getChildren().removeLast();
        }
    }

    @FXML
    void show_search() {
        if (search_box.getChildren().size() == 1) {
            if (mainController.gradedDataLoader.getStudentData().size() > boxes.size()) {
                var entry = mainController.gradedDataLoader.getStudentData().lastEntry();
                boxes.add(makeStudent(entry.getKey(), entry.getValue().name()));
            }
            search_box.getChildren().add(box);
        }
    }

    @FXML
    void input() {
        show_search();
        String filter = inputField.getText();
        if (filter.isEmpty()) {
            searchCrossIcon.setImage(new Image(GradedResourceLoader.load("icons/search.svg")));
        } else {
            searchCrossIcon.setImage(new Image(GradedResourceLoader.load("icons/close.svg")));
        }
        if (filter.isEmpty()) {
            filteredData.setPredicate(s -> true);
        } else {
            filteredData.setPredicate(s -> {
                if (s.getId() != null)
                    return s.getId().toUpperCase().contains(filter.toUpperCase());

                return true;
            });
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            if (choiceBox != null) {
                choiceBox.setItems(FXCollections.observableArrayList(List.of(
                        "Eng", "Math", "Comp", "Phy", "Chem", "Bio", "SST"
                )));
            }
            var x = new FXMLLoader(loadURL("fxml/list-for-search.fxml"));
            observableList = generate();
            filteredData = new FilteredList<>(observableList, s -> true);
            listViewStudents = new ListViewStudents(this);
            x.setControllerFactory(c -> listViewStudents);
            box = x.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ObservableList<HBox> generate() {
        var l = mainController.gradedDataLoader.getStudentData();
        for (var x : l.keySet()) {
            boxes.add(makeStudent(x, l.get(x).name()));
        }
        return FXCollections.observableList(boxes);
    }

    private static HBox makeStudent(String edNumber, String studentName) {
        Label ed = new Label(edNumber);
        ed.setMinWidth(50);
        Label name = new Label(studentName);
        HBox hBox = new HBox(ed, name);
        hBox.setSpacing(30);
        hBox.setId(edNumber + " " + studentName);
        hBox.getStyleClass().add("hbox");
        return hBox;
    }

    @FXML
    public void doAction(ActionEvent event) {
        Button source = (Button) event.getSource();
        if (!inputField.getText().isEmpty()) {
            updateAttendance(source, true, null);
        }

    }

    public void updateHomeWorkStatus(String val) {
        String edNo = listViewStudents.ed;
        String date = LocalDate.now().toString();
        try (
                Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE Attendance SET homework = ? WHERE ed_no = ? and date = ?")) {
            updateStmt.setString(1, val);
            updateStmt.setString(2, edNo);
            updateStmt.setString(3, date);
            updateStmt.executeUpdate();
            String msg = """
                    🎉 Homework Completed!
                    Dear Parent,
                    Great news! Your child %s has successfully completed their homework for today.
                    We’re proud of their effort and commitment to learning. Keep up the great work! 🌟
                    """.formatted(mainController.gradedDataLoader.getStudentData().get(edNo).name());
            if (mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id() != null) {
                try {
                    mainController.messageSender.sendMessage(msg, Long.parseLong(mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id()));
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("Message was not sent to the server.");
                }

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAttendance(Button source, boolean shouldMessageBeSend, String updatedTime) {
        String timeStamp = updatedTime == null ? LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")) : updatedTime;
        Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
        String edNo = listViewStudents.ed;
        String date = LocalDate.now().toString();
        try {
            if (source.getText().equals("Check In")) {
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE Attendance SET check_in = ?  WHERE ed_no = ? and date= ?");
                updateStmt.setString(2, edNo);
                updateStmt.setString(3, date);
                updateStmt.setString(1, timeStamp);
                attendanceMap.get(edNo).setCheck_in(timeStamp);
                listViewStudents.attendanceDataView.update();
                String msg = """
                        Arrival Alert
                        Dear Parent,
                        Your child %s has safely arrived at their tuition center at %s.
                        Thank you for trusting us with their learning journey!
                        """.formatted(mainController.gradedDataLoader.getStudentData().get(edNo).name(), timeStamp);
                if (mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id() != null && shouldMessageBeSend) {
                    Platform.runLater(() -> {
                        try {
                            if (mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id() != null) {
                                mainController.messageSender.sendMessage(msg, Long.parseLong(mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id()));
                                mainController.sendNotification("Arrival message was sent successfully for " + edNo, Styles.SUCCESS);

                            }

                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            System.out.println("Message was not sent to the server.");
                            mainController.sendNotification("Message was not sent to the server for " + edNo, Styles.DANGER);

                        }
                    });
                }
                source.setText("Check Out");
                inputField.setText("");
                updateStmt.executeUpdate();
            } else if (source.getText().equals("Check Out")) {
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE Attendance SET check_out = ?  WHERE ed_no = ? and date= ?");
                updateStmt.setString(2, edNo);
                updateStmt.setString(3, date);
                updateStmt.setString(1, timeStamp);
                attendanceMap.get(edNo).setCheck_out(timeStamp);
                source.setVisible(false);
                listViewStudents.attendanceDataView.update();
                String msg = """
                        Departure Alert
                        Dear Parent,
                        Your child %s has just left the tuition center at %s.
                        We hope they had a great learning experience today. See you next time!
                        """.formatted(mainController.gradedDataLoader.getStudentData().get(edNo).name(), timeStamp);
                Platform.runLater(() -> {
                    try {
                        if (mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id() != null && shouldMessageBeSend) {
                            mainController.messageSender.sendMessage(msg, Long.parseLong(mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id()));
                            mainController.sendNotification("Departure message was sent successfully for " + edNo, Styles.SUCCESS);
                        }

                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        System.out.println("Message was not sent to the server.");
                        mainController.sendNotification("Message was not sent to the server for " + edNo, Styles.DANGER);

                    }
                });
                inputField.setText("");
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onCutOrSearch(MouseEvent mouseEvent) {
        searchCrossIcon.setImage(new Image(GradedResourceLoader.load("icons/search.svg")));
        inputField.setText("");
    }
}
