package org.graded_classes.graded_attendance.controller.quiz;


import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.components.OMRParser;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.ExamData;
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ConductExam implements Initializable {
    MainController mainController;

    @FXML
    private TableColumn<ExamData, HBox> action;
    @FXML
    private TableColumn<ExamData, Button> generate, add;

    @FXML
    private TableColumn<ExamData, String> classes, doe, id, room, subject, time, topic;
    @FXML
    private TableView<ExamData> scheduleTable;
    ObservableList<ExamData> items = FXCollections.observableList(new ArrayList<>());

    @FXML
    private Label today;
    private final ArrayList<ExamData> examSchedular;

    public ConductExam(MainController mainController, ArrayList<ExamData> examSchedular) {
        this.mainController = mainController;
        this.examSchedular = examSchedular;
    }

    @FXML
    void addNewExam() {
        mainController.modalPane.setPersistent(true);
        Node node = mainController.gradedFxmlLoader.createView(R.exam_create, new ExamCreator(mainController, this));
        mainController.modalPane.show(node);
    }

    @FXML
    void addDiagnosticExam() {
        mainController.modalPane.setPersistent(true);
        Node node = mainController.gradedFxmlLoader.createView(R.diagnostic_test, new DiagnosticTest(mainController, this));
        mainController.modalPane.show(node);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        today.setText(LocalDate.now().getDayOfMonth() + " " +
                format(LocalDate.now().getMonth().toString()) +
                " " + LocalDate.now().getYear());
        classes.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().classes()));
        doe.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().doe()));
        id.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().id()));
        room.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().room()));
        subject.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().subject()));
        time.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().time()));
        topic.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().topic_name()));
        action.setCellValueFactory(arg0 -> {
            Button button = new Button("Start Exam");
            Button result = new Button("Generate Result");
            button.setPadding(new Insets(5, 5, 5, 5));
            button.getStyleClass().add(Styles.SUCCESS);
            result.setPadding(new Insets(5, 5, 5, 5));
            result.getStyleClass().add(Styles.SUCCESS);
            ExamData examInfo = arg0.getValue();
            HBox hBox = new HBox(button, result);
            hBox.setAlignment(Pos.CENTER);
            hBox.setSpacing(5);
            button.setOnMouseClicked(_ -> {
                var sub = examInfo.subject();
                if (!containsMoreThanOneSubject(sub)) {
                    mainController.modalPane.show(mainController.
                            gradedFxmlLoader
                            .createView(R.exam_entry_login,
                                    new LoginBeforeEntry(mainController, examInfo, "Normal")));
                } else {
                    mainController.modalPane.show(mainController.
                            gradedFxmlLoader
                            .createView(R.exam_entry_login,
                                    new LoginBeforeEntry(mainController, examInfo, "Diagnostic")));
                }
            });
            result.setOnAction(_ -> {
                if (containsMoreThanOneSubject(examInfo.subject())) {
                    generateDiagnosticResultAsync(
                            examInfo,
                            result
                    );
                    Node node = mainController
                            .gradedFxmlLoader
                            .createView(R.diagnostic_exam_report, new DiagnosticExamReport(mainController));
                    node.setStyle("-fx-background-color: #fafafa");

                    if (node instanceof Region region) {
                        region.setPrefSize(900, 700);
                        region.setMaxSize(900, 700);
                    }

                    mainController.modalPane.show(node);

                } else {

                    generateNormalResultAsync(
                            examInfo,
                            result
                    );

                }
            });
            return new SimpleObjectProperty<>(hBox);
        });
        add.setCellValueFactory(args -> {
            Button button = new Button("Add exam OMR");
            ExamData examInfo = args.getValue();
            var questionMap = loadQuestion(examInfo);
            button.setOnMouseClicked(_ -> {
                File generatedFile = fileChooser();

                CompletableFuture.runAsync(() -> {
                    var omrData = OMRParser.parse(generatedFile.getAbsolutePath());
                    System.out.println(omrData);
                    List<String> notFoundStudents = new ArrayList<>();

                    for (var roll : omrData.keySet()) {
                        System.out.println(roll);
                        boolean exists = mainController.gradedDataLoader
                                .getStudentData()
                                .containsKey(roll);

                        if (!exists) {
                            notFoundStudents.add(roll);
                            continue;
                        }
                        System.out.println( omrData.get(roll).values().stream().toList());
                        saveFromOMR(
                                roll,
                                omrData.get(roll).values().stream().toList(),
                                questionMap,
                                examInfo
                        );
                    }

                    if (!notFoundStudents.isEmpty()) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Student Not Found");
                            alert.setHeaderText(
                                    notFoundStudents.size() + " student(s) not found"
                            );
                            alert.setContentText(
                                    String.join(", ", notFoundStudents)
                            );
                            alert.show();
                        });
                    }
                });
            });
            return new SimpleObjectProperty<>(button);
        });
        generate.setCellValueFactory(cellData -> {
            Button button = new Button("Exam Pdf");

            button.getStyleClass().add(Styles.ACCENT);
            button.setPadding(new Insets(8));

            ExamData examInfo = cellData.getValue();

            button.setOnAction(_ -> generateExamPdf(examInfo, button));

            return new SimpleObjectProperty<>(button);
        });
        for (var sec : examSchedular) {
            items.addAll(sec);
        }
        scheduleTable.setItems(items);
    }

    private void generateNormalResultAsync(
            ExamData examInfo,
            Button sourceButton
    ) {
        sourceButton.setDisable(true);
        sourceButton.setText("Generating...");

        CompletableFuture
                .runAsync(() -> generateResult(examInfo))
                .whenComplete((unused, throwable) ->
                        Platform.runLater(() -> {

                            sourceButton.setDisable(false);
                            sourceButton.setText("Generate Result");

                            if (throwable != null) {
                                showResultAlert(
                                        Alert.AlertType.ERROR,
                                        "Result Generation Failed",
                                        "Unable to generate the normal exam result.",
                                        getThrowableMessage(throwable)
                                );
                                return;
                            }

                            showResultAlert(
                                    Alert.AlertType.INFORMATION,
                                    "Result Generated",
                                    "Normal exam results generated successfully.",
                                    "The ScoreCard table has been updated."
                            );
                        })
                );
    }

    private boolean containsMoreThanOneSubject(String sub) {
        return (sub.contains("[")
                && sub.contains("]"));
    }

    private void generateDiagnosticResultAsync(
            ExamData examInfo,
            Button sourceButton
    ) {
        sourceButton.setDisable(true);
        sourceButton.setText("Generating...");

        CompletableFuture
                .supplyAsync(() -> generateDiagnosticResult(examInfo))
                .whenComplete((summary, throwable) ->
                        Platform.runLater(() -> {

                            sourceButton.setDisable(false);
                            sourceButton.setText("Generate Result");

                            if (throwable != null) {
                                showResultAlert(
                                        Alert.AlertType.ERROR,
                                        "Diagnostic Result Failed",
                                        "Unable to generate diagnostic results.",
                                        getThrowableMessage(throwable)
                                );
                                return;
                            }

                            showResultAlert(
                                    Alert.AlertType.INFORMATION,
                                    "Diagnostic Result Generated",
                                    "Diagnostic results generated successfully.",
                                    summary
                            );
                        })
                );
    }

    private void showResultAlert(
            Alert.AlertType type,
            String title,
            String header,
            String message
    ) {
        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        if (scheduleTable.getScene() != null) {
            alert.initOwner(
                    scheduleTable.getScene().getWindow()
            );
        }

        alert.show();
    }

    private String getThrowableMessage(Throwable throwable) {

        Throwable cause = throwable;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage() == null
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }

    private void generateResult(ExamData examInfo) {

        var listOfStudents = mainController.gradedDataLoader.getStudentData()
                .sequencedValues()
                .stream()
                .filter(student -> student._class().equals(examInfo.classes())).
                filter(student -> student.getBoard().equalsIgnoreCase(examInfo.board()) ||
                        examInfo.board().equals("Other"))
                .toList();

        System.out.println("Exam Result , Subject: " + examInfo.subject() + " ,Class: " + examInfo.classes());
        System.out.println("Conducted on " + examInfo.doe() + " , Topic Name:  " + examInfo.topic_name());

        var questionDataMap = loadQuestion(examInfo);

        String sql = """
                INSERT INTO ScoreCard
                (
                    exam_id,
                    ed_no,
                    subject,
                    topic_name,
                    marks_obtain,
                    total_marks,
                    remark
                )
                VALUES
                (
                    ?, ?, ?, ?, ?, ?, ?
                )
                ON CONFLICT(exam_id, ed_no)
                DO UPDATE SET
                    subject = excluded.subject,
                    topic_name = excluded.topic_name,
                    marks_obtain = excluded.marks_obtain,
                    total_marks = excluded.total_marks,
                    remark = excluded.remark
                """;
        var con = mainController.gradedDataLoader.databaseLoader.getConnection();
        try (var ps = con.prepareStatement(sql)) {

            for (var st : listOfStudents) {

                int score = 0;

                var map = getAnswers(Integer.parseInt(examInfo.id()), st.ed_no());

                for (var n : map.keySet()) {
                    int opId = map.get(n);
                    var question = questionDataMap.get("" + n);

                    if (question != null
                            && question.option_data() != null
                            && question.option_data().option_index() == opId) {
                        score = score + 4;
                    }
                }

                String remark = (score <= 0)
                        ? "Absent or not eligible"
                        : "Present";

                int totalMarks = 80;

                System.out.println(
                        st.ed_no() + ":    " +
                                ((score <= 0)
                                        ? "Absent or not eligible"
                                        : score + "/80" + "       Name: " + st.name())
                );

                ps.setInt(1, Integer.parseInt(examInfo.id()));
                ps.setString(2, st.ed_no());
                ps.setString(3, examInfo.subject());
                ps.setString(4, examInfo.topic_name());
                ps.setInt(5, score);
                ps.setInt(6, totalMarks);
                ps.setString(7, remark);

                ps.addBatch();
            }

            ps.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TreeMap<String, QuestionData> loadQuestion(ExamData examData) {
        TreeMap<String, QuestionData> map =
                new TreeMap<>(Comparator.comparingInt(Integer::parseInt));
        try {
            var connection = mainController.gradedDataLoader.databaseLoader.getConnection();
            var sql = """
                    select *
                    from ExamQuestion
                             join Questions on Questions.question_id = ExamQuestion.question_id
                    where exam_id = %s
                    """.
                    formatted(examData.id());
            java.sql.ResultSet rs;
            try {
                PreparedStatement pst = connection.prepareStatement(sql);
                rs = pst.executeQuery();
                while (rs.next()) {
                    var inner_sql = "Select * from QuestionOptions where question_id=?";
                    PreparedStatement _pst = connection.prepareStatement(inner_sql);
                    String questionId = rs.getString("question_id");
                    _pst.setString(1, questionId);
                    var _rs = _pst.executeQuery();
                    OptionData questionOption = null;
                    LinkedHashMap<Integer, String> options = new LinkedHashMap<>();
                    int correctId = 0;
                    while (_rs.next()) {
                        options.put(_rs.getInt("option_id"), _rs.getString("option_text"));
                        var id = _rs.getInt("is_correct");
                        if (id == 1)
                            correctId = options.size();
                    }
                    questionOption = new OptionData(correctId, options);
                    QuestionData questionData = new QuestionData(questionId,
                            rs.getString("topic_id"),
                            rs.getString("user_id"),
                            rs.getString("date_of_making"),
                            rs.getString("type"),
                            rs.getString("level"),
                            rs.getString("question_txt"),
                            rs.getString("question_img_path"), questionOption);
                    map.put(questionData.question_id(), questionData);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return map;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
    private record DiagnosticQuestionInfo(
            int questionId,
            String subject,
            String topicName,
            int correctOptionIndex
    ) {
    }
    private List<DiagnosticQuestionInfo> loadDiagnosticQuestionInfo(
            int examId
    ) {
        String sql = """
            SELECT
                q.question_id,
                TRIM(t.subject) AS subject,
                TRIM(t.topic_name) AS topic_name,
                qo.option_order AS correct_option_index
            FROM ExamQuestion eq
            INNER JOIN Questions q
                ON q.question_id = eq.question_id
            INNER JOIN Topics t
                ON t.topic_id = q.topic_id
            INNER JOIN QuestionOptions qo
                ON qo.question_id = q.question_id
               AND qo.is_correct = 1
            WHERE eq.exam_id = ?
            ORDER BY
                t.subject COLLATE NOCASE,
                q.question_id
            """;

        Connection connection = mainController
                .gradedDataLoader
                .databaseLoader
                .getConnection();

        List<DiagnosticQuestionInfo> questions = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, examId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String subject = normalizeDiagnosticSubject(
                            rs.getString("subject")
                    );

                    if (subject.isBlank()) {
                        subject = "Unspecified";
                    }

                    String topicName = rs.getString("topic_name");

                    questions.add(
                            new DiagnosticQuestionInfo(
                                    rs.getInt("question_id"),
                                    subject,
                                    topicName == null
                                            ? ""
                                            : topicName.trim(),
                                    rs.getInt("correct_option_index")
                            )
                    );
                }
            }

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Failed to load diagnostic exam questions.",
                    exception
            );
        }

        return questions;
    }
    public static TreeMap<Integer, Integer> getAnswers(int examId, String studentEd) {

        TreeMap<Integer, Integer> answersMap = new TreeMap<>();
        String DB_URL = "jdbc:sqlite:" + Main.getRootPath() + "GradeEd_Exam_2026/" + studentEd + ".db";

        String sql = "SELECT question_id, selected_option_id FROM answers WHERE exam_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int questionId = rs.getInt("question_id");
                int selectedOptionId = rs.getInt("selected_option_id");

                answersMap.put(questionId, selectedOptionId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return answersMap;
    }
    private String normalizeDiagnosticSubject(String subject) {

        if (subject == null) {
            return "";
        }

        String normalized = subject.trim();

        if (normalized.equalsIgnoreCase("Mathematics")
                || normalized.equalsIgnoreCase("Maths")) {
            return "Math";
        }

        if (normalized.equalsIgnoreCase("Phy")) {
            return "Physics";
        }

        if (normalized.equalsIgnoreCase("Chem")) {
            return "Chemistry";
        }

        if (normalized.equalsIgnoreCase("Bio")) {
            return "Biology";
        }

        if (normalized.equalsIgnoreCase("Eng")) {
            return "English";
        }

        return normalized;
    }
    private static final class DiagnosticSubjectResult {

        private final String subject;
        private final Set<String> topicNames =
                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        private int questionCount;
        private int attempted;
        private int correct;
        private int wrong;

        private DiagnosticSubjectResult(String subject) {
            this.subject = subject;
        }

        private void addQuestion(String topicName) {
            questionCount++;

            if (topicName != null && !topicName.isBlank()) {
                topicNames.add(topicName.trim());
            }
        }

        private void addCorrectAnswer() {
            attempted++;
            correct++;
        }

        private void addWrongAnswer() {
            attempted++;
            wrong++;
        }

        private String joinedTopicNames() {
            return String.join(", ", topicNames);
        }
    }
    private String generateDiagnosticResult(ExamData examInfo) {

        int examId = Integer.parseInt(examInfo.id());

        File diagnosisDirectory = getDiagnosisDirectory();

        if (!diagnosisDirectory.exists()) {
            throw new IllegalStateException(
                    "Diagnosis directory does not exist:\n"
                            + diagnosisDirectory.getAbsolutePath()
            );
        }

        if (!diagnosisDirectory.isDirectory()) {
            throw new IllegalStateException(
                    "Diagnosis path is not a directory:\n"
                            + diagnosisDirectory.getAbsolutePath()
            );
        }

        File[] diagnosticDatabases = diagnosisDirectory.listFiles(file -> {

            if (!file.isFile()) {
                return false;
            }

            String fileName = file
                    .getName()
                    .toLowerCase(Locale.ROOT);

            return !fileName.endsWith("-wal")
                    && !fileName.endsWith("-shm")
                    && !fileName.endsWith("-journal");
        });

        if (diagnosticDatabases == null
                || diagnosticDatabases.length == 0) {

            throw new IllegalStateException(
                    "No diagnostic student databases were found in:\n"
                            + diagnosisDirectory.getAbsolutePath()
            );
        }

        Arrays.sort(
                diagnosticDatabases,
                Comparator.comparing(
                        File::getName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        /*
         * Each question contains:
         * - question ID
         * - subject from Topics.subject
         * - topic name from Topics.topic_name
         * - correct option index
         */
        List<DiagnosticQuestionInfo> examQuestions =
                loadDiagnosticQuestionInfo(examId);

        if (examQuestions.isEmpty()) {
            throw new IllegalStateException(
                    "No questions were found for diagnostic exam "
                            + examInfo.id()
            );
        }

        /*
         * Store questions by ID for fast answer lookup.
         */
        Map<Integer, DiagnosticQuestionInfo> questionById =
                new HashMap<>();

        for (DiagnosticQuestionInfo question : examQuestions) {
            questionById.put(question.questionId(), question);
        }

        /*
         * Count the number of questions available for each subject.
         * TreeMap keeps the subjects ordered alphabetically.
         */
        Map<String, List<DiagnosticQuestionInfo>> questionsBySubject =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (DiagnosticQuestionInfo question : examQuestions) {

            questionsBySubject
                    .computeIfAbsent(
                            question.subject(),
                            ignored -> new ArrayList<>()
                    )
                    .add(question);
        }

        final int marksPerQuestion = 4;

        String sql = """
            INSERT INTO DiagnosticScoreCard
            (
                exam_id,
                student_name,
                subject,
                topic_name,
                marks_obtain,
                total_marks,
                total_attempted,
                correct_answers,
                wrong_answers,
                remark
            )
            VALUES
            (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            ON CONFLICT(exam_id, student_name, subject)
            DO UPDATE SET
                topic_name = excluded.topic_name,
                marks_obtain = excluded.marks_obtain,
                total_marks = excluded.total_marks,
                total_attempted = excluded.total_attempted,
                correct_answers = excluded.correct_answers,
                wrong_answers = excluded.wrong_answers,
                remark = excluded.remark,
                generated_at = CURRENT_TIMESTAMP
            """;

        Connection connection = mainController
                .gradedDataLoader
                .databaseLoader
                .getConnection();

        int generatedStudentCount = 0;
        int generatedRowCount = 0;
        int skippedCount = 0;

        List<String> skippedStudents = new ArrayList<>();

        boolean previousAutoCommit;

        try {
            previousAutoCommit = connection.getAutoCommit();
        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Failed to read database transaction state.",
                    exception
            );
        }

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(sql)) {

                for (File databaseFile : diagnosticDatabases) {

                    String studentName =
                            getStudentNameFromDatabaseFile(databaseFile);

                    if (studentName == null || studentName.isBlank()) {
                        skippedCount++;
                        skippedStudents.add(databaseFile.getName());
                        continue;
                    }

                    studentName = studentName.trim();

                    DiagnosticAnswerReadResult readResult =
                            getDiagnosticAnswers(examId, databaseFile);

                    if (!readResult.success()) {
                        skippedCount++;

                        skippedStudents.add(
                                studentName
                                        + ": "
                                        + readResult.errorMessage()
                        );

                        continue;
                    }

                    TreeMap<Integer, Integer> selectedAnswers =
                            readResult.answers();

                    if (selectedAnswers == null) {
                        selectedAnswers = new TreeMap<>();
                    }

                    /*
                     * Create an accumulator for every subject before checking
                     * answers. Therefore, an unattempted subject still receives
                     * its own DiagnosticScoreCard row.
                     */
                    Map<String, DiagnosticSubjectResult> subjectResults =
                            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

                    for (DiagnosticQuestionInfo question : examQuestions) {

                        DiagnosticSubjectResult subjectResult =
                                subjectResults.computeIfAbsent(
                                        question.subject(),
                                        DiagnosticSubjectResult::new
                                );

                        subjectResult.addQuestion(question.topicName());
                    }

                    /*
                     * Evaluate the answers.
                     */
                    for (Map.Entry<Integer, Integer> answer
                            : selectedAnswers.entrySet()) {

                        int questionId = answer.getKey();

                        Integer selectedOptionValue = answer.getValue();

                        if (selectedOptionValue == null
                                || selectedOptionValue <= 0) {
                            continue;
                        }

                        DiagnosticQuestionInfo question =
                                questionById.get(questionId);

                        if (question == null) {
                            System.err.println(
                                    "Diagnostic answer does not belong "
                                            + "to the current exam."
                                            + " Exam ID: " + examId
                                            + ", student: " + studentName
                                            + ", question ID: " + questionId
                            );

                            continue;
                        }

                        DiagnosticSubjectResult result =
                                subjectResults.get(question.subject());

                        if (selectedOptionValue
                                == question.correctOptionIndex()) {

                            result.addCorrectAnswer();

                        } else {
                            result.addWrongAnswer();
                        }
                    }

                    /*
                     * Insert one database row for every subject.
                     */
                    for (DiagnosticSubjectResult result
                            : subjectResults.values()) {

                        int subjectTotalMarks =
                                result.questionCount * marksPerQuestion;

                        int subjectObtainedMarks =
                                result.correct * marksPerQuestion;

                        String remark = result.attempted == 0
                                ? "Not attempted"
                                : "Present";

                        ps.setInt(1, examId);
                        ps.setString(2, studentName);
                        ps.setString(3, result.subject);
                        ps.setString(4, result.joinedTopicNames());
                        ps.setInt(5, subjectObtainedMarks);
                        ps.setInt(6, subjectTotalMarks);
                        ps.setInt(7, result.attempted);
                        ps.setInt(8, result.correct);
                        ps.setInt(9, result.wrong);
                        ps.setString(10, remark);

                        ps.addBatch();
                        generatedRowCount++;

                        System.out.println(
                                "Diagnostic result"
                                        + " | Exam: " + examId
                                        + " | Name: " + studentName
                                        + " | Subject: " + result.subject
                                        + " | Score: "
                                        + subjectObtainedMarks
                                        + "/"
                                        + subjectTotalMarks
                                        + " | Attempted: "
                                        + result.attempted
                                        + " | Correct: "
                                        + result.correct
                                        + " | Wrong: "
                                        + result.wrong
                                        + " | Remark: "
                                        + remark
                        );
                    }

                    generatedStudentCount++;
                }

                if (generatedRowCount > 0) {
                    ps.executeBatch();
                }

                connection.commit();
            }

        } catch (SQLException exception) {

            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }

            throw new RuntimeException(
                    "Failed to save diagnostic results.",
                    exception
            );

        } finally {

            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException exception) {
                System.err.println(
                        "Failed to restore auto-commit: "
                                + exception.getMessage()
                );
            }
        }

        int completeExamTotalMarks =
                examQuestions.size() * marksPerQuestion;

        StringBuilder summary = new StringBuilder();

        summary.append("Exam ID: ")
                .append(examInfo.id())
                .append("\nStudents processed: ")
                .append(generatedStudentCount)
                .append("\nSubject rows generated: ")
                .append(generatedRowCount)
                .append("\nSubjects: ")
                .append(String.join(", ", questionsBySubject.keySet()))
                .append("\nSkipped databases: ")
                .append(skippedCount)
                .append("\nComplete exam marks: ")
                .append(completeExamTotalMarks);

        for (Map.Entry<String, List<DiagnosticQuestionInfo>> entry
                : questionsBySubject.entrySet()) {

            int subjectMarks =
                    entry.getValue().size() * marksPerQuestion;

            summary.append("\n")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue().size())
                    .append(" questions, ")
                    .append(subjectMarks)
                    .append(" marks");
        }

        if (!skippedStudents.isEmpty()) {
            summary.append("\n\nSkipped databases:\n")
                    .append(String.join("\n", skippedStudents));
        }

        return summary.toString();
    }

    private File getDiagnosisDirectory() {

        /*
         * Main.getRootPath() should normally end with / or \.
         * Using File(parent, child) works even if it does not.
         */
        return new File(
                Main.getRootPath(),
                "diagnosis"
        );
    }

    private String getStudentNameFromDatabaseFile(File databaseFile) {

        String fileName = databaseFile.getName();

        String[] knownExtensions = {
                ".sqlite3",
                ".sqlite",
                ".db"
        };

        for (String extension : knownExtensions) {

            if (fileName.toLowerCase(Locale.ROOT)
                    .endsWith(extension)) {

                fileName = fileName.substring(
                        0,
                        fileName.length() - extension.length()
                );

                break;
            }
        }

        return normalizeStudentName(fileName);
    }

    private String normalizeStudentName(String name) {

        if (name == null) {
            return "";
        }

        return name
                .trim()
                .replaceAll("\\s+", " ");
    }

    private record DiagnosticAnswerReadResult(
            boolean success,
            TreeMap<Integer, Integer> answers,
            String errorMessage
    ) {
    }

    private DiagnosticAnswerReadResult getDiagnosticAnswers(
            int examId,
            File databaseFile
    ) {
        TreeMap<Integer, Integer> answersMap =
                new TreeMap<>();

        String databaseUrl =
                "jdbc:sqlite:" + databaseFile.getAbsolutePath();

        String sql = """
                SELECT
                    question_id,
                    selected_option_id
                FROM answers
                WHERE exam_id = ?
                """;

        try (
                Connection connection =
                        DriverManager.getConnection(databaseUrl);

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, examId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    int questionId =
                            resultSet.getInt("question_id");

                    int selectedOptionId =
                            resultSet.getInt("selected_option_id");

                    answersMap.put(
                            questionId,
                            selectedOptionId
                    );
                }
            }

            return new DiagnosticAnswerReadResult(
                    true,
                    answersMap,
                    null
            );

        } catch (SQLException exception) {

            exception.printStackTrace();

            return new DiagnosticAnswerReadResult(
                    false,
                    answersMap,
                    exception.getMessage()
            );
        }
    }

    private String format(String date) {
        return date.charAt(0) + date.substring(1).toLowerCase();
    }

    private void generateExamPdf(
            ExamData examInfo,
            Button sourceButton
    ) {
        sourceButton.setDisable(true);
        sourceButton.setText("Loading...");

        CompletableFuture
                .supplyAsync(() -> {
                    var questionMap = loadQuestion(examInfo);
                    System.out.println(questionMap);
                    return new ArrayList<>(questionMap.values());
                })
                .whenComplete((questionList, throwable) ->
                        Platform.runLater(() -> {
                            sourceButton.setDisable(false);
                            sourceButton.setText("Exam Pdf");

                            if (throwable != null) {
                                showPdfError(
                                        "Unable to load exam questions.",
                                        throwable
                                );
                                return;
                            }

                            if (questionList == null
                                    || questionList.isEmpty()) {
                                showPdfError(
                                        "This exam does not contain any questions.",
                                        null
                                );
                                return;
                            }

                            try {
                                Window owner = scheduleTable
                                        .getScene()
                                        .getWindow();

                                File generatedFile =
                                        ExamPdfGenerator
                                                .chooseLocationAndGenerate(
                                                        owner,
                                                        examInfo,
                                                        questionList
                                                );

                                if (generatedFile != null) {
                                    Alert alert =
                                            new Alert(
                                                    Alert.AlertType.INFORMATION
                                            );

                                    alert.setTitle("Exam PDF");
                                    alert.setHeaderText(
                                            "Exam PDF generated successfully"
                                    );
                                    alert.setContentText(
                                            generatedFile.getAbsolutePath()
                                    );

                                    alert.initOwner(owner);
                                    alert.show();
                                }

                            } catch (Exception exception) {
                                showPdfError(
                                        "Unable to generate the exam PDF.",
                                        exception
                                );
                            }
                        })
                );
    }

    private void showPdfError(
            String message,
            Throwable throwable
    ) {
        if (throwable != null) {
            throwable.printStackTrace();
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exam PDF Error");
        alert.setHeaderText(message);

        if (throwable != null) {
            Throwable cause = throwable.getCause() != null
                    ? throwable.getCause()
                    : throwable;

            alert.setContentText(
                    cause.getMessage() == null
                            ? cause.getClass().getSimpleName()
                            : cause.getMessage()
            );
        }

        if (scheduleTable.getScene() != null) {
            alert.initOwner(scheduleTable.getScene().getWindow());
        }

        alert.show();
    }

    public void saveFromOMR(String studentEd, List<String> selectedOption,
                            TreeMap<String, QuestionData> map, ExamData examInfo) {
        String endTime = LocalTime.now().toString();

        String url = "jdbc:sqlite:" + Main.getRootPath() + "GradeEd_Exam_2026/" + studentEd + ".db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            int index = 0;
            for (QuestionData question : map.values()) {
                String createTableSQL = """
                        INSERT INTO answers (
                            exam_id,
                            question_id,
                            selected_option_id,
                            time_slot,
                            start_time,
                            end_time,
                            created_at
                        ) VALUES (%s, %s, %s, '%s', '%s', '%s', '%s');
                        """.formatted(
                        examInfo.id(),
                        question.question_id(),
                        convertToNumeric(selectedOption.get(index++)),
                        "",
                        "",
                        endTime,
                        LocalDate.now()
                );
                stmt.execute(createTableSQL);
            }
        } catch (Exception e) {

        }
    }

    private int convertToNumeric(String s) {
        return switch (s) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 0;
        };
    }

    public File fileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Option Image Selector");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.txt")
        );
        return fileChooser.showOpenDialog(mainController.getStage());
    }

}
