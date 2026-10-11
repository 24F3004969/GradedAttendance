package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.SearchField;
import com.dlsc.gemsfx.TimePicker;
import impl.com.calendarfx.view.NumericTextField;
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
import java.sql.*;
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
    private NumericTextField questionCount;
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
            "CBSE", "ICSE", "Both"

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

        String insertExamSql = """
            INSERT INTO ExamScheduler (
                subject,
                class,
                exam_date,
                start_time,
                end_time,
                room_no,
                board
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String insertExamTopicSql = """
            INSERT INTO ExamTopic (
                exam_id,
                topic_id
            )
            VALUES (?, ?)
            """;

        Connection conn = null;
        boolean previousAutoCommit = true;

        try {
            conn = mainController
                    .gradedDataLoader
                    .databaseLoader
                    .getConnection();

            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // -----------------------------------------------------
            // Read and validate the form values
            // -----------------------------------------------------

            String classValue = classNum.getValue();
            String subjectValue = subject.getSelectedItem();
            String roomValue = roomNo.getValue();
            String boardValue = board.getValue();

            if (classValue == null || classValue.isBlank()) {
                throw new IllegalStateException("Please select a class.");
            }

            if (subjectValue == null || subjectValue.isBlank()) {
                throw new IllegalStateException("Please select a subject.");
            }

            if (exam.getValue() == null) {
                throw new IllegalStateException("Please select the exam date.");
            }

            if (startTime.getTime() == null) {
                throw new IllegalStateException("Please select the start time.");
            }

            if (endTime.getTime() == null) {
                throw new IllegalStateException("Please select the end time.");
            }

            String examDate = exam.getValue().toString();
            String start = startTime.getTime().toString();
            String end = endTime.getTime().toString();

            // -----------------------------------------------------
            // Get selected topic names and corresponding IDs
            // -----------------------------------------------------

            String topicNameCsv = getAsCSW(topicList);

            List<String> topicNames = Arrays.stream(topicNameCsv.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .toList();

            if (topicNames.isEmpty()) {
                throw new IllegalStateException(
                        "Please select at least one topic."
                );
            }

            List<Integer> topicIds = getTopicIds(
                    conn,
                    topicNames,
                    subjectValue,
                    classValue
            ).stream()
                    .distinct()
                    .toList();

            if (topicIds.isEmpty()) {
                throw new IllegalStateException(
                        "No matching topic IDs were found."
                );
            }

            /*
             * This prevents continuing when some selected topic names
             * could not be resolved to IDs.
             *
             * This assumes topic names are unique for a particular
             * subject and class.
             */
            if (topicIds.size() != topicNames.size()) {
                throw new IllegalStateException(
                        "Some selected topics could not be matched. " +
                                "Selected topics: " + topicNames +
                                ", matched topic IDs: " + topicIds
                );
            }

            System.out.println("Selected topic names: " + topicNames);
            System.out.println("Selected topic IDs: " + topicIds);

            // -----------------------------------------------------
            // Insert the main ExamScheduler record
            // -----------------------------------------------------

            int generatedExamId;

            try (PreparedStatement pstmt = conn.prepareStatement(
                    insertExamSql,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                pstmt.setString(1, subjectValue);
                pstmt.setString(2, classValue);
                pstmt.setString(3, examDate);
                pstmt.setString(4, start);
                pstmt.setString(5, end);
                pstmt.setString(6, roomValue);
                pstmt.setString(7, boardValue);

                int insertedRows = pstmt.executeUpdate();

                if (insertedRows != 1) {
                    throw new SQLException(
                            "Failed to insert the exam schedule."
                    );
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException(
                                "Exam was inserted, but no generated ID was returned."
                        );
                    }

                    generatedExamId = generatedKeys.getInt(1);
                }
            }

            // -----------------------------------------------------
            // Insert all selected topics into ExamTopic
            // -----------------------------------------------------

            try (PreparedStatement pstmt = conn.prepareStatement(
                    insertExamTopicSql
            )) {
                for (Integer topicId : topicIds) {
                    pstmt.setInt(1, generatedExamId);
                    pstmt.setInt(2, topicId);
                    pstmt.addBatch();
                }

                int[] topicInsertResults = pstmt.executeBatch();

                for (int result : topicInsertResults) {
                    if (result == Statement.EXECUTE_FAILED) {
                        throw new SQLException(
                                "Failed to assign one or more topics to the exam."
                        );
                    }
                }
            }

            // -----------------------------------------------------
            // Create exam questions using all selected topics
            // -----------------------------------------------------

            createExamQuestion(
                    conn,
                    generatedExamId,
                    topicIds
            );

            // Save everything only after every step succeeds.
            conn.commit();

            System.out.println(
                    "Exam successfully created with ID: " + generatedExamId
            );

            return new ExamData(
                    String.valueOf(generatedExamId),
                    classValue,
                    examDate,
                    roomValue,
                    subjectValue,
                    start + " - " + end,
                    topicIds,
                    topicNames,
                    boardValue
            );

        } catch (Exception e) {

            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println(
                            "Exam creation failed. Database changes were rolled back."
                    );
                } catch (SQLException rollbackException) {
                    rollbackException.addSuppressed(e);
                    rollbackException.printStackTrace();
                }
            }

            e.printStackTrace();
            return null;

        } finally {

            if (conn != null) {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
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
        System.out.println(Integer.parseInt(questionCount.getText()));
        // 3. Batch insert up to 20 randomized questions into ExamQuestion
        try (PreparedStatement innerPst = conn.prepareStatement(insertSql)) {
            for (int i = 0; i < questionIds.size() && i < Integer.parseInt(questionCount.getText()); i++) {
                int quesId = questionIds.get(i);

                innerPst.setInt(1, generatedId);
                innerPst.setInt(2, quesId);
                System.out.println(generatedId+"     "+quesId);
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
