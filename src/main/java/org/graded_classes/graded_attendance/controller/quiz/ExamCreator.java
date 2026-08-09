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
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.ExamData;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

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
    private HBox topics;
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
    public ArrayList<String> topicList = new ArrayList<>();
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
                (topic_id, subject, class, exam_date, start_time, end_time, room_no,board)
                VALUES (?, ?, ?, ?, ?, ?, ?,?)
                """;

        try {
            Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();

            PreparedStatement pstmt = conn.prepareStatement(
                    sql,
                    PreparedStatement.RETURN_GENERATED_KEYS
            );

            String classValue = classNum.getValue();
            String subjectValue = subject.getSelectedItem();
            String roomValue = roomNo.getValue();

            String examDate = exam.getValue().toString();
            String start = startTime.getTime().toString();
            String end = endTime.getTime().toString();

            // Get selected topic names as CSV, for display/storage in ExamData
            String topicName = getAsCSW(topicList);

            // Convert CSV topic names into List<String>
            List<String> topicNames = Arrays.stream(topicName.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            // Get all matching topic IDs
            List<Integer> topicIds = getTopicIds(conn, topicNames, subjectValue, classValue);

            if (topicIds.isEmpty()) {
                System.out.println("No matching topic IDs found.");
                return null;
            }

            System.out.println("Topic IDs: " + topicIds);

            /*
             * ExamScheduler currently accepts only one topic_id.
             * So we save the first topic_id here.
             * All topic IDs will still be used for question selection below.
             */
            int mainTopicId = topicIds.get(0);

            pstmt.setInt(1, mainTopicId);
            pstmt.setString(2, subjectValue);
            pstmt.setString(3, classValue);
            pstmt.setString(4, examDate);
            pstmt.setString(5, start);
            pstmt.setString(6, end);
            pstmt.setString(7, roomValue);
            pstmt.setString(8, board.getValue());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();

            int generatedId = -1;

            if (rs.next()) {
                generatedId = rs.getInt(1);
            }

            if (generatedId == -1) {
                System.out.println("Failed to get generated exam ID.");
                return null;
            }

            // Now create exam questions using all selected topic IDs
            createExamQuestion(conn, generatedId, topicIds);

            return new ExamData(
                    "" + generatedId,
                    classValue,
                    examDate,
                    roomValue,
                    subjectValue,
                    start + "-" + end,
                    topicIds.toString(),
                    topicName,
                    board.getValue()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getAsCSW(List<String> topicList) {
        if (topicList == null) {
            return "";
        }
        return String.join(",", topicList);
    }


    private void createExamQuestion(Connection conn, int generatedId, List<Integer> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            System.out.println("No topics found. Exam questions not created.");
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(topicIds.size(), "?"));

        String selectSql = "SELECT question_id FROM Questions WHERE topic_id IN (" + placeholders + ")";
        String insertSql = "INSERT INTO ExamQuestion (exam_id, question_id) VALUES (?, ?)";

        List<Integer> questionIds = new ArrayList<>();

        // 1. Fetch all question IDs matching the topic IDs
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            for (int i = 0; i < topicIds.size(); i++) {
                pstmt.setInt(i + 1, topicIds.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questionIds.add(rs.getInt("question_id"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (questionIds.isEmpty()) {
            System.out.println("No questions found for selected topics.");
            return;
        }

        // 2. Randomize the sequence of the question IDs
        Collections.shuffle(questionIds);

        // 3. Batch insert up to 20 randomized questions into ExamQuestion
        try (PreparedStatement innerPst = conn.prepareStatement(insertSql)) {

            for (int i = 0; i < questionIds.size() && i < 20; i++) {
                int quesId = questionIds.get(i);

                innerPst.setInt(1, generatedId);
                innerPst.setInt(2, quesId);
                innerPst.addBatch();
            }

            innerPst.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getTopicIds(Connection conn, List<String> topics, String subject, String className) {
        List<Integer> topicIds = new ArrayList<>();

        if (topics == null || topics.isEmpty()) {
            System.out.println("No topic names provided.");
            return topicIds;
        }

        String placeholders = String.join(",", Collections.nCopies(topics.size(), "?"));

        String sql = """
                SELECT topic_id
                FROM Topics
                WHERE topic_name IN (%s)
                  AND subject = ?
                  AND class = ?
                """.formatted(placeholders);

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int index = 1;

            // Set topic names
            for (String topic : topics) {
                pstmt.setString(index++, topic);
            }

            // Set subject and class
            pstmt.setString(index++, subject);
            pstmt.setString(index, className);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topicIds.add(rs.getInt("topic_id"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return topicIds;
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
        topicName.setOnCommit(event -> {
            if (!topicList.contains(event)) {
                topicList.add(event);
                addTopicList(event);
            }
        });
    }

    public void addTopicList(String topicName) {
        Label label = new Label(topicName);
        var graphics = new FontIcon(Material2AL.CLOSE);
        label.setGraphic(graphics);
        label.setStyle("-fx-border-color:rgba(0, 0, 0, 0.2);-fx-padding: 5;-fx-background-radius: 5;-fx-border-radius: 5;");
        topics.getChildren().add(label);
        graphics.setOnMouseClicked((event) -> {
            FontIcon fontIcon = (FontIcon) event.getSource();
            Label lab = (Label) fontIcon.getParent();
            topics.getChildren().remove(lab);
            topicList.remove(lab.getText());
        });

    }
}
