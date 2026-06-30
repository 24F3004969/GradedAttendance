package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.SearchField;
import com.dlsc.gemsfx.TimePicker;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.data.ExamData;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class ExamCreator implements Initializable {
    @FXML
    private ComboBox<String> classNum;
    @FXML
    private ComboBox<String> board;
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
    ObservableList<String> observableListSubject = FXCollections.observableArrayList(List.of());
    ObservableList<String> observableListTopics = FXCollections.observableArrayList(List.of());
    ObservableList<String> observableListClasses = FXCollections.observableArrayList(List.of());
    ObservableList<String> observableListBoard = FXCollections.observableArrayList(List.of(
            "CBSE", "ICSE", "Other"

    ));
    ObservableList<String> observableListRoom = FXCollections.observableArrayList(List.of(
            "3E", "3D", "3A"

    ));
    MainController mainController;
    ConductExam conductExam;

    public ExamCreator(MainController mainController, ConductExam conductExam) {
        this.mainController = mainController;
        this.conductExam = conductExam;
        init();

    }

    public void init() {
        TreeSet<String> classSet = new TreeSet<>();
        TreeSet<String> subjectSet = new TreeSet<>();
        TreeSet<String> topicSet = new TreeSet<>();
        try {
            Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from Topics");

            while (rs.next()) {
                classSet.add(rs.getString("class"));
                subjectSet.add(rs.getString("subject"));
                topicSet.add(rs.getString("topic_name"));
            }
            observableListClasses.addAll(classSet);
            observableListSubject.addAll(subjectSet);
            observableListTopics.addAll(topicSet);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    void create() {
        var exam = initDb();
        if (exam != null) {
            conductExam.items.add(exam);
        }
        mainController.modalPane.hide();
    }

    private ExamData initDb() {
        String sql = """
                    INSERT INTO ExamScheduler
                    (topic_id, subject, class, exam_date, start_time, end_time, room_no)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try {
            Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            String classValue = classNum.getValue();
            String subjectValue = subject.getSelectedItem();
            String topicName = this.topicName.getSelectedItem();
            String roomValue = roomNo.getValue();

            String examDate = exam.getValue().toString();
            String start = startTime.getTime().toString();
            String end = endTime.getTime().toString();
            int topicId = getTopicId(conn, topicName, subjectValue, classValue);
            pstmt.setInt(1, topicId);
            pstmt.setString(2, subjectValue);
            pstmt.setString(3, classValue);
            pstmt.setString(4, examDate);
            pstmt.setString(5, start);
            pstmt.setString(6, end);
            pstmt.setString(7, roomValue);

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            int generatedId = -1;
            if (rs.next()) {
                generatedId = rs.getInt(1);
            }
            createExamQuestion(conn, generatedId, topicId);
            return new ExamData(
                    "" + generatedId,
                    classValue,
                    examDate,
                    roomValue,
                    subjectValue,
                    start + "-" + end,
                    "" + topicId,
                    topicName
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void createExamQuestion(Connection conn, int generatedId, int topicId) {
        String selectSql = "SELECT question_id FROM Questions WHERE topic_id = ?";
        String insertSql = "INSERT INTO ExamQuestion (exam_id, question_id) VALUES (?, ?)";

        List<Integer> questionIds = new ArrayList<>();

        // 1. Fetch all question IDs matching the topic
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, topicId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questionIds.add(rs.getInt("question_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return; // Exit if the select query fails
        }

        // 2. Randomize the sequence of the question IDs
        Collections.shuffle(questionIds);

        // 3. Batch insert the randomized questions into ExamQuestion
        try (PreparedStatement innerPst = conn.prepareStatement(insertSql)) {
            for (int i = 0; i < questionIds.size() && i < 20; i++) {
                int quesId = questionIds.get(i);
                innerPst.setInt(1, generatedId);
                innerPst.setInt(2, quesId);
                innerPst.addBatch(); // Add to batch instead of executing one by one
            }
            innerPst.executeBatch(); // Execute all insertions at once
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private int getTopicId(Connection conn, String topic, String subject, String className) {

        String sql = """
                    SELECT topic_id
                    FROM Topics
                    WHERE topic_name = ?
                      AND subject = ?
                      AND class = ?
                    LIMIT 1
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, topic);
            pstmt.setString(2, subject);
            pstmt.setString(3, className);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("topic_id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        close.setOnMouseClicked(event -> {
            this.mainController.modalPane.hide();
        });
        topicName.setSuggestionProvider(request ->
                observableListTopics.stream().filter(country ->
                                country.toLowerCase().contains(request.getUserText().toLowerCase())).
                        collect(Collectors.toList()));
        subject.setSuggestionProvider(request -> observableListSubject.stream().filter(country ->
                country.toLowerCase().contains(request.getUserText().toLowerCase())).collect(Collectors.toList()));
        classNum.setItems(observableListClasses);
        board.setItems(observableListBoard);
        roomNo.setItems(observableListRoom);

    }
}
