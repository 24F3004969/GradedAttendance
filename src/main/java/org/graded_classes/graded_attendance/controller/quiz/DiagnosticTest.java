package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.SearchField;
import com.dlsc.gemsfx.TimePicker;
import impl.com.calendarfx.view.NumericTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class DiagnosticTest implements Initializable {

    @FXML
    private ComboBox<String> classNum;
    @FXML
    private ComboBox<String> board;
    @FXML
    private Button close;
    @FXML
    private HBox subjects;
    @FXML
    private TimePicker endTime;
    @FXML
    private DatePicker exam;
    @FXML
    private NumericTextField questionCount;
    @FXML
    private ComboBox<String> roomNo;

    @FXML
    private TimePicker startTime;

    @FXML
    private SearchField<String> subject;
    public ArrayList<String> subjectList = new ArrayList<>();
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

    public DiagnosticTest(MainController mainController, ConductExam conductExam) {
        this.mainController = mainController;
        this.conductExam = conductExam;
        init();

    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        close.setOnMouseClicked(event -> {
            this.mainController.modalPane.hide();
        });
        subject.setSuggestionProvider(request -> observableListSubject.stream().filter(country ->
                country.toLowerCase().contains(request.getUserText().toLowerCase())).collect(Collectors.toList()));
        classNum.setItems(observableListClasses);
        board.setItems(observableListBoard);
        roomNo.setItems(observableListRoom);
        subject.setOnCommit(event -> {
            if (!subjectList.contains(event)) {
                subjectList.add(event);
                addSubjectList(event);
            }
        });
    }

    @FXML
    void create() {
        var exam = initDb();
        if (exam != null) {
            conductExam.items.add(exam);
        }
        mainController.modalPane.hide();
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

    public void addSubjectList(String subjectName) {
        Label label = new Label(subjectName);
        var graphics = new FontIcon(Material2AL.CLOSE);
        label.setGraphic(graphics);
        label.setStyle("-fx-border-color:rgba(0, 0, 0, 0.2);-fx-padding: 5;-fx-background-radius: 5;-fx-border-radius: 5;");
        subjects.getChildren().add(label);
        graphics.setOnMouseClicked((event) -> {
            FontIcon fontIcon = (FontIcon) event.getSource();
            Label lab = (Label) fontIcon.getParent();
            subjects.getChildren().remove(lab);
            subjectList.remove(lab.getText());
        });

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
            String roomValue = roomNo.getValue();

            String examDate = exam.getValue().toString();
            String start = startTime.getTime().toString();
            String end = endTime.getTime().toString();
            List<Integer> topicIds = getTopicIds(conn, classValue);

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
            pstmt.setString(2, subjectList.toString());
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
            createExamQuestion(conn, generatedId);

            return new ExamData(
                    "" + generatedId,
                    classValue,
                    examDate,
                    roomValue,
                    subjectList.toString(),
                    start + "-" + end,
                    topicIds.toString(),
                    "Diagnosis",
                    board.getValue()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private List<Integer> getTopicIds(Connection conn,
                                      String classValue) {
        List<Integer> topicIds = new ArrayList<>();

        if (subjectList == null || subjectList.isEmpty()) {
            return topicIds;
        }

        String placeholders = String.join(",",
                Collections.nCopies(subjectList.size(), "?"));

        String sql = "SELECT topic_id " +
                "FROM Topics " +
                "WHERE class = ? " +
                "AND topic_name = ? " +
                "AND subject IN (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;
            ps.setString(index++, classValue);
            ps.setString(index++, "Diagnosis");

            for (String subject : subjectList) {
                ps.setString(index++, subject);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topicIds.add(rs.getInt("topic_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return topicIds;
    }

    private void createExamQuestion(Connection conn, int generatedId) {

        if (subjectList == null || subjectList.isEmpty()) {
            System.out.println("No subjects found.");
            return;
        }

        String placeholders =
                String.join(",", Collections.nCopies(subjectList.size(), "?"));

        String selectSql = """
                SELECT q.question_id
                FROM Questions q
                INNER JOIN Topics t
                    ON q.topic_id = t.topic_id
                WHERE t.class = ?
                  AND t.topic_name = 'Diagnosis'
                  AND t.subject IN (%s)
                """.formatted(placeholders);

        String insertSql =
                "INSERT INTO ExamQuestion (exam_id, question_id) VALUES (?, ?)";

        List<Integer> questionIds = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            int index = 1;

            // global class variable
            pstmt.setString(index++, classNum.getValue());

            // global subjectList variable
            for (String subject : subjectList) {
                pstmt.setString(index++, subject);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questionIds.add(rs.getInt("question_id"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        if (questionIds.isEmpty()) {
            System.out.println("No Diagnosis questions found.");
            return;
        }

        Collections.shuffle(questionIds);

        int limit = Integer.parseInt(questionCount.getText());

        try (PreparedStatement pst = conn.prepareStatement(insertSql)) {

            for (int i = 0; i < Math.min(limit, questionIds.size()); i++) {
                pst.setInt(1, generatedId);
                pst.setInt(2, questionIds.get(i));
                pst.addBatch();
            }

            pst.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
