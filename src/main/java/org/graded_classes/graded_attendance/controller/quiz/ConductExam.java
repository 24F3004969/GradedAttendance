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
            button.setOnMouseClicked(_ -> mainController.modalPane.show(mainController.
                    gradedFxmlLoader
                    .createView(R.exam_entry_login,
                            new LoginBeforeEntry(mainController, examInfo))));
            result.setOnMouseClicked(_ -> {
                generateResult(examInfo);
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

                    List<String> notFoundStudents = new ArrayList<>();

                    for (var roll : omrData.keySet()) {

                        boolean exists = mainController.gradedDataLoader
                                .getStudentData()
                                .containsKey(roll);

                        if (!exists) {
                            notFoundStudents.add(roll);
                            continue;
                        }

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

    private void generateResult(ExamData examInfo) {

        var listOfStudents = mainController.gradedDataLoader.getStudentData()
                .sequencedValues()
                .stream()
                .filter(student -> student._class().equals(examInfo.classes())).
                filter(student -> student.getBoard().equalsIgnoreCase(examInfo.board()) ||
                        examInfo.board().equals("Both"))
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
        TreeMap<String, QuestionData> map = new TreeMap<>();
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
                    /*
                     * loadQuestion currently returns:
                     * TreeMap<String, QuestionData>
                     */
                    var questionMap = loadQuestion(examInfo);

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

