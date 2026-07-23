package org.graded_classes.graded_attendance.controller.home;

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
import org.graded_classes.graded_attendance.data.DailyTopics;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.graded_classes.graded_attendance.GradedResourceLoader.loadURL;

public class StudentAttendance implements Initializable {
    @FXML
    public ImageView searchCrossIcon;
    @FXML
    TextField inputField;
    @FXML
    ComboBox<String> choiceBox;
    VBox box;
    private String msg;
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
    public String todayTopicList = "";
    ListViewStudents listViewStudents;
    final String submit = """
             Dear Parent,
            We are pleased to inform you that your child has brought the assigned homework to the coaching class today.
            Thank you for your support in ensuring timely completion.
            """;
    final String not_submit = """
            Dear Parent,
            We wanted to inform you that your child did not bring the assigned homework today.
            Kindly ensure that the homework is completed and brought to coaching tomorrow. 
            """;
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
        loadAttendanceData();
    }

    private void loadAttendanceData() {
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
                                getAsRequired(r.getString("homework")),
                                r.getString("topic_taught")));
            }
        } catch (SQLException _) {

        }
    }


    private Boolean getAsRequired(String homework) {
        return homework != null ? homework.equals("Submitted") : null;
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
            Button feeReportButton = (Button) outer_main_box.
                    getParent().lookup("#reportButton");
            FontIcon icon = (FontIcon) feeReportButton.getGraphic();
            if (id.equals("st_fee"))
                icon.getStyleClass().set(0,"fee-report-icon");
            else
                icon.getStyleClass().set(0,"camera-icon");

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

    public void updateAttendance(Button source, boolean shouldMessageBeSend, String updatedTime) {
        String timeStamp = updatedTime == null ? LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")) : updatedTime;
        Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
        String edNo = listViewStudents.ed;
        String studentClass = mainController.gradedDataLoader.getStudentData().get(edNo)._class();
        TreeMap<String, DailyTopics> data = new TreeMap<>();
        if (attendanceMap.get(edNo).getTopics() == null) {
            try {
                data = DailyTopicsDao.loadForDateAllClasses(mainController.gradedDataLoader.databaseLoader.getConnection(),
                        LocalDate.now());
                if (!isAllNull(data.get(studentClass))) {
                    var valid = data.get(studentClass);
                    topicTaughtTodayUpdate(valid.getSubject1(), valid.getTopic1(), valid.getSubject2(), valid.getTopic2());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        String date = LocalDate.now().toString();
        try {
            if (source.getText().equals("Check In")) {
                todayTopicList = "";
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE Attendance SET check_in = ?  WHERE ed_no = ? and date= ?");
                updateStmt.setString(2, edNo);
                updateStmt.setString(3, date);
                updateStmt.setString(1, timeStamp);
                attendanceMap.get(edNo).setCheck_in(timeStamp);
                if (!(data.isEmpty()))
                    attendanceMap.get(edNo).setTopics(data.get(studentClass).getSubject1() + ":" + data.get(studentClass).getTopic1() +
                            data.get(studentClass).getSubject2() + ":" + data.get(studentClass).getTopic2());
                else
                    attendanceMap.get(edNo).setTopics("Unknown");
                listViewStudents.attendanceDataView.update();
                String msg = """
                        Arrival Alert
                        Dear Parent,
                        Your child %s has safely arrived at their tuition center at %s.
                        Thank you for trusting us with their learning journey!
                        """.formatted(mainController.gradedDataLoader.getStudentData().get(edNo).name(), timeStamp);
                if (mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id() != null && shouldMessageBeSend) {

                    CompletableFuture.runAsync(() -> {
                        try {
                            if (mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id() != null) {
                                mainController.messageSender.sendMessage(
                                        msg,
                                        Long.parseLong(mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id())
                                );
                                Platform.runLater(() ->
                                        mainController.sendNotification("Arrival message was sent successfully for " + edNo, Styles.SUCCESS)
                                );
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            System.out.println("Message was not sent to the server.");
                            Platform.runLater(() ->
                                    mainController.sendNotification("Message was not sent to the server for " + edNo, Styles.DANGER)
                            );
                        }
                    });
                }
                source.setText("Check Out");
                inputField.setText("");
                updateStmt.executeUpdate();
            } else if (source.getText().equals("Check Out") && !todayTopicList.isEmpty() &&
                    (listViewStudents.attendanceDataView.Submitted.isSelected() ||
                            listViewStudents.attendanceDataView.NotSubmitted.isSelected())) {

                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE Attendance SET check_out = ?, homework = ? WHERE ed_no = ? AND date = ?"
                );
                updateStmt.setString(1, timeStamp);
                updateStmt.setString(2, listViewStudents.attendanceDataView.Submitted.isSelected() ? "Submitted" : "NotSubmitted");
                updateStmt.setString(3, edNo);
                updateStmt.setString(4, date);
                attendanceMap.get(edNo).setCheck_out(timeStamp);
                source.setVisible(false);
                attendanceMap.get(edNo).setHomework_status(listViewStudents.attendanceDataView.Submitted.isSelected());
                listViewStudents.attendanceDataView.update();
                String[] list = todayTopicList.contains(",") ? todayTopicList.split("[,:]") : todayTopicList.split(":");
                System.out.println(Arrays.toString(list));
                if (todayTopicList.contains(","))
                    msg = """
                            Departure Alert
                            Dear Parent,
                            Your child %s has just left the tuition center at %s.
                            
                            Topic Taught Today
                            1.%s : %s
                            2.%s : %s
                            3.Homework : %s
                            
                            %s
                            
                            We hope they had a great learning experience today. See you next time!
                            
                            """.formatted(mainController.gradedDataLoader.getStudentData().get(edNo).name(), timeStamp,
                            list[0], list[1], list[2],
                            list[3], listViewStudents.attendanceDataView.Submitted.isSelected() ? "Submitted" : "Not Submitted",
                            listViewStudents.attendanceDataView.Submitted.isSelected() ? submit : not_submit);
                else {
                    msg = """
                            Departure Alert
                            Dear Parent,
                            Your child %s has just left the tuition center at %s.
                            
                            
                            Topic Taught Today
                            1.%s : %s
                            2.Homework : %s
                            %s
                            
                            We hope they had a great learning experience today. See you next time!
                            """.formatted(mainController.gradedDataLoader.getStudentData().
                                    get(edNo).name(), timeStamp,
                            list[0], list[1], listViewStudents.attendanceDataView.Submitted.isSelected() ? "Submitted" : "Not Submitted",
                            listViewStudents.attendanceDataView.Submitted.isSelected() ? submit : not_submit);
                }

                CompletableFuture.runAsync(() -> {
                    try {
                        if (mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id() != null && shouldMessageBeSend) {
                            mainController.messageSender.sendMessage(
                                    msg,
                                    Long.parseLong(mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id())
                            );
                            Platform.runLater(() ->
                                    mainController.sendNotification("Departure message was sent successfully for " + edNo, Styles.SUCCESS)
                            );
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        System.out.println("Message was not sent to the server.");
                        Platform.runLater(() ->
                                mainController.sendNotification("Message was not sent to the server for " + edNo, Styles.DANGER)
                        );
                    }
                });
                inputField.setText("");
                updateStmt.executeUpdate();
                todayTopicList = "";
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isAllNull(DailyTopics dailyTopics) {

        return dailyTopics.getSubject1() == null && dailyTopics.getSubject2() == null && dailyTopics.getSubject3() == null &&
                dailyTopics.getTopic1() == null && dailyTopics.getTopic2() == null && dailyTopics.getTopic3() == null;
    }

    @FXML
    public void onCutOrSearch(MouseEvent mouseEvent) {
        searchCrossIcon.setImage(new Image(GradedResourceLoader.load("icons/search.svg")));
        inputField.setText("");
    }

    public void topicTaughtTodayUpdate(String subject1, String topic1, String subject2, String topic2) {
        Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
        String edNo = listViewStudents.ed;
        String date = LocalDate.now().toString();
        PreparedStatement updateStmt = null;
        try {

            updateStmt = conn.prepareStatement("UPDATE Attendance SET topic_taught = ?  WHERE ed_no = ? and date= ?");
            updateStmt.setString(2, edNo);
            updateStmt.setString(3, date);
            if (!subject1.isEmpty() && !topic1.isEmpty() && !subject2.isEmpty() && !topic2.isEmpty())
                todayTopicList = subject1 + ":" + topic1 + "," + subject2 + ":" + topic2;
            else if (!subject2.isEmpty() && !topic2.isEmpty()) {
                todayTopicList = subject2 + ":" + topic2;

            } else if (!subject1.isEmpty() && !topic1.isEmpty()) {
                todayTopicList = subject1 + ":" + topic1;
            }
            updateStmt.setString(1, todayTopicList);
            updateStmt.executeUpdate();
            attendanceMap.get(edNo).setTopics(todayTopicList);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
}
