package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.*;
import com.dlsc.gemsfx.daterange.DateRange;
import com.dlsc.gemsfx.daterange.DateRangePicker;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.components.OMRParser;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.ExamData;
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConductExam implements Initializable {

     final MainController mainController;
    private final ArrayList<ExamData> examSchedular;
    private final ExamAnswerReviewGenerator reviewGenerator;
    private final ExamReviewSender reviewSender;
    @FXML
    public Button deleteSelectedBtn;

    @FXML
    private SimpleFilterView filters;
    @FXML
    private TableColumn<ExamData, HBox> action;
    @FXML
    private TableColumn<ExamData, HBox> generate;
    @FXML
    private TableColumn<ExamData, HBox> add;
    @FXML
    private TableColumn<ExamData, String> classes;
    @FXML
    private TableColumn<ExamData, String> doe;
    @FXML
    private TableColumn<ExamData, String> id;
    @FXML
    private TableColumn<ExamData, String> room;
    @FXML
    private TableColumn<ExamData, String> subject;
    @FXML
    private TableColumn<ExamData, String> time;
    @FXML
    private TableColumn<ExamData, String> topic;
    @FXML
    private TableColumn<ExamData, HBox> share;
    @FXML
    private TableColumn<ExamData, CheckBox> checkboxes;
    @FXML
    private TableView<ExamData> scheduleTable;
    @FXML
    private Label today;

    final ObservableList<ExamData> items = FXCollections.observableArrayList();

    public ConductExam(
            MainController mainController,
            ArrayList<ExamData> examSchedular
    ) {
        this.mainController = Objects.requireNonNull(
                mainController,
                "MainController cannot be null."
        );
        this.examSchedular = Objects.requireNonNull(
                examSchedular,
                "Exam scheduler list cannot be null."
        );
        this.reviewGenerator = new ExamAnswerReviewGenerator();
        this.reviewSender = new ExamReviewSender(mainController);
    }

    private void updateDeleteButton() {

        long count = 8;

        deleteSelectedBtn.setVisible(count > 0);
        deleteSelectedBtn.setManaged(count > 0);

        deleteSelectedBtn.setText(
                "Delete (" + count + ")"
        );
    }

    @FXML
    void addNewExam() {
        mainController.modalPane.setPersistent(true);
        Node node = mainController.gradedFxmlLoader.createView(
                R.exam_create,
                new ExamCreator(mainController, this)
        );
        mainController.modalPane.show(node);
    }

    @FXML
    void addDiagnosticExam() {
        mainController.modalPane.setPersistent(true);
        Node node = mainController.gradedFxmlLoader.createView(
                R.diagnostic_test,
                new DiagnosticTest(mainController, this)
        );
        mainController.modalPane.show(node);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LocalDate now = LocalDate.now();
        today.setText(
                now.getDayOfMonth()
                        + " "
                        + format(now.getMonth().toString())
                        + " "
                        + now.getYear()
        );

        configureTextColumns();
        configureActionColumn();
        configureShareColumn();
        configureOmrColumn();
        configureExamPdfColumn();
        configureCheckboxColumn();
        configureContextMenu();
        configureFilterView();
        items.setAll(examSchedular);
        scheduleTable.setItems(items);

    }

    public record Status(int id, String name) {
        @NotNull
        @Override
        public String toString() {
            return this.name;
        }
    }

    private void configureFilterView() {
        CustomTextField search = filters.addSearchTextField("Search");
        SelectionBox<Status> status =
                filters.addSelectionBox("Filter");

        status.getItems().addAll(
                new Status(1, "Topics"),
                new Status(2, "Subject"),
                new Status(3, "Class"),
                new Status(3, "Room No.")
        );
        status.setGraphic(new FontIcon(Feather.FILTER));

        DateRangePicker range = filters.addDateRangePicker("Date Range");
        ChipsViewContainer chips = new ChipsViewContainer();
        chips.chipsProperty().bind(filters.chipsProperty());
        chips.setOnClear(filters::clear);
        filters.setOnChange(() -> reload(status.getSelectionModel().getSelectedItems(),
                range.getValue(), search.getText()));
    }

    public static <T> void autoResizeColumns(TableView<T> table) {
        for (TableColumn<T, ?> column : table.getColumns()) {

            Text text = new Text(column.getText());
            double max = text.getLayoutBounds().getWidth();

            for (int i = 0; i < table.getItems().size(); i++) {

                Object cellData = column.getCellData(i);

                if (cellData != null) {
                    text = new Text(cellData.toString());

                    double width = text.getLayoutBounds().getWidth();

                    if (width > max) {
                        max = width;
                    }
                }
            }

            column.setPrefWidth(max + 30);
        }
    }

    private void reload(ObservableList<Status> selectedItems, DateRange value, String text) {

    }

    private void configureTextColumns() {
        classes.setCellValueFactory(value ->
                new SimpleStringProperty(value.getValue().classes()));
        doe.setCellValueFactory(value ->
                new SimpleStringProperty(value.getValue().doe()));
        id.setCellValueFactory(value ->
                new SimpleStringProperty(value.getValue().id()));
        room.setCellValueFactory(value ->
                new SimpleStringProperty(value.getValue().room()));
        subject.setCellValueFactory(value ->
                new SimpleStringProperty(value.getValue().subject()));
        time.setCellValueFactory(value ->
                new SimpleStringProperty(value.getValue().time()));
        topic.setCellValueFactory(value ->
                new SimpleStringProperty(
                        value.getValue().topicNamesForDisplay()
                ));
    }

    private void configureActionColumn() {
        action.setCellValueFactory(cell -> {
            ExamData exam = cell.getValue();

            Button startButton = new Button("Start Exam");
            Button resultButton = new Button("Generate Result");
            startButton.getStyleClass().addAll(
                    "exam-action-btn",
                    "start-btn"
            );
            resultButton.getStyleClass().addAll(
                    "exam-action-btn",
                    "result-btn"
            );
            startButton.setPadding(new Insets(5));
            resultButton.setPadding(new Insets(5));


            startButton.setOnAction(event ->
                    mainController.modalPane.show(
                            mainController.gradedFxmlLoader.createView(
                                    R.exam_entry_login,
                                    new LoginBeforeEntry(
                                            mainController,
                                            exam,
                                            containsMoreThanOneSubject(exam.subject())
                                                    ? "Diagnostic"
                                                    : "Normal"
                                    )
                            )
                    )
            );

            resultButton.setOnAction(event -> {
                if (containsMoreThanOneSubject(exam.subject())) {
                    generateDiagnosticResultAsync(exam, resultButton);
                    showDiagnosticReport();
                } else {
                    generateNormalResultAsync(exam, resultButton);
                }
            });


            HBox box = new HBox(5, startButton, resultButton);
            box.setAlignment(Pos.CENTER);
            return new SimpleObjectProperty<>(box);
        });
    }

    private void showDiagnosticReport() {
        Node node = mainController.gradedFxmlLoader.createView(
                R.diagnostic_exam_report,
                new DiagnosticExamReport(mainController)
        );
        node.setStyle("-fx-background-color: #fafafa");

        if (node instanceof Region region) {
            region.setPrefSize(900, 700);
            region.setMaxSize(900, 700);
        }

        mainController.modalPane.show(node);
    }

    private void configureContextMenu() {

        scheduleTable.setRowFactory(tv -> {

            TableRow<ExamData> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem startExam = new MenuItem("Start Exam");
            startExam.setGraphic(icon(Feather.PLAY));

            startExam.setOnAction(e -> {
                ExamData exam = row.getItem();

                if (exam != null) {
                    System.out.println("Start Exam : "
                            + exam.topicNamesForDisplay());

                    // startExam(exam);
                }
            });
            MenuItem generateResult = new MenuItem("Generate Result");
            generateResult.setGraphic(icon(Feather.BAR_CHART_2));

            generateResult.setOnAction(e -> {
                ExamData exam = row.getItem();

                if (exam != null) {
                    System.out.println("Generate Result : "
                            + exam.topicNamesForDisplay());

                    // generateResult(exam);
                }
            });
            MenuItem info = new MenuItem("Info");
            info.setGraphic(icon(Feather.INFO));

            info.setOnAction(e -> {
                ExamData exam = row.getItem();

                if (exam != null) {
                    System.out.println("Generate PDF : "
                            + exam.topicNamesForDisplay());

                    // info(exam);
                }
            });

            MenuItem preview = new MenuItem("Preview");
            preview.setGraphic(icon(Feather.EYE));

            preview.setOnAction(e -> {
                ExamData exam = row.getItem();
                var node = mainController.gradedFxmlLoader.createView(R.exam_preview);
                mainController.modalPane.show(node);

            });
            MenuItem edit = new MenuItem("Edit");
            edit.setGraphic(icon(Feather.EDIT_2));

            edit.setOnAction(e -> {
                ExamData exam = row.getItem();

                if (exam != null) {
                    System.out.println("Edit : "
                            + exam.topicNamesForDisplay());

                    // editExam(exam);
                }
            });

            MenuItem duplicate = new MenuItem("Duplicate");
            duplicate.setGraphic(icon(Feather.COPY));

            duplicate.setOnAction(e -> {
                ExamData exam = row.getItem();

                if (exam != null) {
                    System.out.println("Duplicate : "
                            + exam.topicNamesForDisplay());
                }
            });
            FontIcon deleteIcon = icon(Feather.TRASH_2);
            MenuItem delete = createDelete(deleteIcon, row);
            Menu exportMenu = new Menu("Export");
            exportMenu.setGraphic(icon(Feather.DOWNLOAD));

            MenuItem exportPdf = new MenuItem("PDF");
            exportPdf.setGraphic(icon(Feather.FILE_TEXT));

            MenuItem exportExcel = new MenuItem("Excel");
            exportExcel.setGraphic(icon(Feather.GRID));

            exportMenu.getItems().addAll(
                    exportPdf,
                    exportExcel
            );

            menu.getItems().addAll(
                    startExam,
                    generateResult,
                    new SeparatorMenuItem(),

                    preview,
                    info,
                    exportMenu,
                    new SeparatorMenuItem(),

                    edit,
                    duplicate,
                    new SeparatorMenuItem(),

                    delete
            );

            // AtlantaFX
            menu.getStyleClass().add(Styles.ACCENT);

            // Select row when right-clicked
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    scheduleTable.getSelectionModel()
                            .select(row.getIndex());
                }
            });

            // Hide context menu for empty rows
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );

            return row;
        });
    }

    @NotNull
    private MenuItem createDelete(FontIcon deleteIcon, TableRow<ExamData> row) {
        MenuItem delete = new MenuItem("Delete");
        delete.setGraphic(deleteIcon);

        delete.setOnAction(e -> {

            ExamData exam = row.getItem();

            if (exam == null) {
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Exam");
            alert.setHeaderText("Delete Selected Exam");
            alert.setContentText(
                    "Are you sure you want to delete\n\n"
                            + exam.topicNamesForDisplay()
            );

            alert.showAndWait()
                    .filter(ButtonType.OK::equals)
                    .ifPresent(btn ->
                            scheduleTable.getItems().remove(exam));
        });
        return delete;
    }

    private FontIcon icon(Feather feather) {
        FontIcon icon = new FontIcon(feather);
        icon.setIconSize(16);
        return icon;
    }

    private void configureCheckboxColumn() {
        checkboxes.setCellValueFactory(cell -> {
            CheckBox box = new CheckBox();
            ExamData exam = cell.getValue();
            box.selectedProperty().setValue(exam.active().getValue());
            box.selectedProperty().addListener((ov, old_val, new_val) -> {
                exam.active().setValue(true);
                updateDeleteButton();
                if (box.isSelected()) {

                } else {


                }
            });
            return new SimpleObjectProperty<>(box);
        });

    }

    private void configureShareColumn() {
        share.setCellValueFactory(cell -> {
            ExamData exam = cell.getValue();
            Button button = new Button("Send Review PDFs");
            button.getStyleClass().addAll(
                    "exam-action-btn",
                    "share-btn"
            );
            button.setPadding(new Insets(8));
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> sendAllReviewReports(exam, button));
            HBox hBox = new HBox(button);
            hBox.setAlignment(Pos.CENTER);
            button.setPadding(new Insets(5));
            return new SimpleObjectProperty<>(hBox);
        });
    }

    private void configureOmrColumn() {
        add.setCellValueFactory(cell -> {
            ExamData exam = cell.getValue();
            Button button = new Button("Add OMR");
            button.getStyleClass().addAll(
                    "exam-action-btn",
                    "omr-btn"
            );
            button.setOnAction(_ -> mainController.modalPane.show(mainController.gradedFxmlLoader.
                    createView(R.omr_checker, new OmrChecker(this, exam)))
            );
            HBox hBox = new HBox(button);
            button.setPadding(new Insets(5));
            hBox.setAlignment(Pos.CENTER);
            return new SimpleObjectProperty<>(hBox);
        });
    }

    private void configureExamPdfColumn() {
        generate.setCellValueFactory(cell -> {
            ExamData exam = cell.getValue();
            Button button = new Button("Exam Pdf");
            button.getStyleClass().addAll(
                    "exam-action-btn",
                    "pdf-btn"
            );
            button.setPadding(new Insets(8));
            button.setOnAction(event -> generateExamPdf(exam, button));
            HBox hBox = new HBox(button);
            hBox.setAlignment(Pos.CENTER);
            button.setPadding(new Insets(5));
            return new SimpleObjectProperty<>(hBox);
        });
    }

    void importOmr(ExamData examInfo) {
        File generatedFile = folderChooser();
        if (generatedFile == null) {
            return;
        }

        TreeMap<String, QuestionData> questionMap = loadQuestion(examInfo);
        var omrData = OMRParser.parse(generatedFile.getAbsolutePath());
        List<String> missingStudents = new ArrayList<>();

        for (var entry : omrData.entrySet()) {
            String enrollmentNumber = entry.getKey();

            if (!mainController.gradedDataLoader
                    .getStudentData()
                    .containsKey(enrollmentNumber)) {
                missingStudents.add(enrollmentNumber);
                continue;
            }

            saveFromOMR(
                    enrollmentNumber,
                    entry.getValue().values().stream().toList(),
                    questionMap,
                    examInfo
            );
        }

        if (!missingStudents.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Student Not Found");
            alert.setHeaderText(
                    missingStudents.size() + " student(s) not found"
            );
            alert.setContentText(String.join(", ", missingStudents));
            initializeOwner(alert);
            alert.show();
        }
    }

    private void generateNormalResultAsync(
            ExamData examInfo,
            Button sourceButton
    ) {
        sourceButton.setDisable(true);
        sourceButton.setText("Generating...");

        CompletableFuture
                .supplyAsync(() -> generateResult(examInfo))
                .whenComplete((directory, throwable) ->
                        Platform.runLater(() -> {
                            sourceButton.setDisable(false);
                            sourceButton.setText("Generate Result");

                            if (throwable != null) {
                                showResultAlert(
                                        Alert.AlertType.ERROR,
                                        "Result Generation Failed",
                                        "Unable to generate the result PDFs.",
                                        getThrowableMessage(throwable)
                                );
                                return;
                            }

                            Alert alert = new Alert(
                                    Alert.AlertType.INFORMATION
                            );
                            alert.setTitle("Result Generated");
                            alert.setHeaderText(
                                    "Results and answer-review PDFs generated successfully."
                            );
                            alert.setContentText(
                                    "The ScoreCard table was updated.\n\n"
                                            + "PDF directory:\n"
                                            + directory.getAbsolutePath()
                            );

                            ButtonType openButton = new ButtonType(
                                    "Open PDF Folder",
                                    ButtonBar.ButtonData.LEFT
                            );
                            alert.getButtonTypes().setAll(
                                    openButton,
                                    ButtonType.OK
                            );
                            initializeOwner(alert);

                            alert.showAndWait()
                                    .filter(openButton::equals)
                                    .ifPresent(button ->
                                            openPdfDirectory(directory));
                        })
                );
    }

    private File generateResult(ExamData examInfo) {
        int examId = parseExamId(examInfo);

        File reviewDirectory = new File(
                Main.getRootPath(),
                "ExamAnswerReviews/Exam_" + examId
        );

        if (!reviewDirectory.exists() && !reviewDirectory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create PDF directory: "
                            + reviewDirectory.getAbsolutePath()
            );
        }

        TreeMap<String, QuestionData> questions = loadQuestion(examInfo);

        if (questions.isEmpty()) {
            throw new IllegalStateException(
                    "No questions found for exam " + examId
            );
        }

        final int marksPerQuestion = 4;
        final int totalMarks = questions.size() * marksPerQuestion;

        var students = mainController.gradedDataLoader
                .getStudentData()
                .sequencedValues()
                .stream()
                .filter(student ->
                        Objects.equals(
                                student._class(),
                                examInfo.classes()
                        ))
                .filter(student ->
                        student.getBoard() != null
                                && examInfo.board() != null
                                && (
                                student.getBoard().equalsIgnoreCase(
                                        examInfo.board()
                                )
                                        || examInfo.board().equalsIgnoreCase(
                                        "Both"
                                )
                        ))
                .toList();

        String overallScoreSql = """
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
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(exam_id, ed_no) DO UPDATE SET
                    subject = excluded.subject,
                    topic_name = excluded.topic_name,
                    marks_obtain = excluded.marks_obtain,
                    total_marks = excluded.total_marks,
                    remark = excluded.remark
                """;

        String topicScoreSql = """
                INSERT INTO TopicWiseScoreCard
                (
                    exam_id,
                    ed_no,
                    topic_id,
                    subject,
                    topic_name,
                    total_questions,
                    attempted_questions,
                    correct_answers,
                    wrong_answers,
                    marks_obtain,
                    total_marks,
                    remark,
                    generated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(exam_id, ed_no, topic_id) DO UPDATE SET
                    subject = excluded.subject,
                    topic_name = excluded.topic_name,
                    total_questions = excluded.total_questions,
                    attempted_questions = excluded.attempted_questions,
                    correct_answers = excluded.correct_answers,
                    wrong_answers = excluded.wrong_answers,
                    marks_obtain = excluded.marks_obtain,
                    total_marks = excluded.total_marks,
                    remark = excluded.remark,
                    generated_at = CURRENT_TIMESTAMP
                """;

        Connection connection = mainController
                .gradedDataLoader
                .databaseLoader
                .getConnection();

        boolean oldAutoCommit;

        try {
            oldAutoCommit = connection.getAutoCommit();
        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Could not read the database transaction state.",
                    exception
            );
        }

        try {
            connection.setAutoCommit(false);

            // Ensures older databases also receive the table.
            createTopicWiseScoreCardTable(connection);

            /*
             * Load topic names once, instead of executing a query separately
             * for every question and every student.
             */
            TreeMap<Integer, String> topicNames =
                    loadTopicNames(connection, questions);

            try (
                    PreparedStatement overallStatement =
                            connection.prepareStatement(overallScoreSql);

                    PreparedStatement topicStatement =
                            connection.prepareStatement(topicScoreSql)
            ) {
                for (var student : students) {
                    TreeMap<Integer, Integer> answers = getAnswers(
                            examId,
                            student.ed_no()
                    );

                    int correctAnswers = calculateCorrectAnswers(
                            questions,
                            answers
                    );

                    int score = correctAnswers * marksPerQuestion;

                    String overallRemark = answers.isEmpty()
                            ? "Absent or not eligible"
                            : "Present";

                    /*
                     * Generate the existing answer-review PDF.
                     */
                    reviewGenerator.generate(
                            examInfo,
                            student.ed_no(),
                            student.name(),
                            questions,
                            answers
                    );

                    /*
                     * Add overall result to ScoreCard batch.
                     */
                    overallStatement.setInt(1, examId);
                    overallStatement.setString(2, student.ed_no());
                    overallStatement.setString(3, examInfo.subject());
                    overallStatement.setString(
                            4,
                            examInfo.topicNamesForDisplay()
                    );
                    overallStatement.setInt(5, score);
                    overallStatement.setInt(6, totalMarks);
                    overallStatement.setString(7, overallRemark);
                    overallStatement.addBatch();

                    /*
                     * Calculate this student's topic-wise results.
                     */
                    List<TopicScore> topicScores =
                            calculateTopicWiseScores(
                                    questions,
                                    answers,
                                    topicNames,
                                    marksPerQuestion
                            );

                    /*
                     * Add one row per topic to the topic result batch.
                     */
                    for (TopicScore topicScore : topicScores) {
                        topicStatement.setInt(1, examId);
                        topicStatement.setString(2, student.ed_no());
                        topicStatement.setInt(
                                3,
                                topicScore.topicId()
                        );
                        topicStatement.setString(
                                4,
                                examInfo.subject()
                        );
                        topicStatement.setString(
                                5,
                                topicScore.topicName()
                        );
                        topicStatement.setInt(
                                6,
                                topicScore.totalQuestions()
                        );
                        topicStatement.setInt(
                                7,
                                topicScore.attemptedQuestions()
                        );
                        topicStatement.setInt(
                                8,
                                topicScore.correctAnswers()
                        );
                        topicStatement.setInt(
                                9,
                                topicScore.wrongAnswers()
                        );
                        topicStatement.setInt(
                                10,
                                topicScore.marksObtained()
                        );
                        topicStatement.setInt(
                                11,
                                topicScore.totalMarks()
                        );
                        topicStatement.setString(
                                12,
                                topicScore.remark()
                        );
                        topicStatement.addBatch();
                    }
                }

                validateBatchResults(
                        overallStatement.executeBatch(),
                        "ScoreCard"
                );

                validateBatchResults(
                        topicStatement.executeBatch(),
                        "TopicWiseScoreCard"
                );
            }

            connection.commit();
            return reviewDirectory;

        } catch (Exception exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }

            throw new RuntimeException(
                    "Failed to generate results for exam ID: " + examId,
                    exception
            );

        } finally {
            try {
                connection.setAutoCommit(oldAutoCommit);
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void createTopicWiseScoreCardTable(
            Connection connection
    ) throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS TopicWiseScoreCard (
                    topic_score_id INTEGER PRIMARY KEY AUTOINCREMENT,
                
                    exam_id INTEGER NOT NULL,
                    ed_no TEXT NOT NULL,
                    topic_id INTEGER NOT NULL,
                
                    subject TEXT NOT NULL,
                    topic_name TEXT NOT NULL,
                
                    total_questions INTEGER NOT NULL DEFAULT 0,
                    attempted_questions INTEGER NOT NULL DEFAULT 0,
                    correct_answers INTEGER NOT NULL DEFAULT 0,
                    wrong_answers INTEGER NOT NULL DEFAULT 0,
                
                    marks_obtain INTEGER NOT NULL DEFAULT 0,
                    total_marks INTEGER NOT NULL DEFAULT 0,
                
                    remark TEXT,
                    generated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                
                    UNIQUE (exam_id, ed_no, topic_id),
                
                    FOREIGN KEY (exam_id)
                        REFERENCES ExamScheduler(exam_id)
                        ON DELETE CASCADE,
                
                    FOREIGN KEY (ed_no)
                        REFERENCES StudentData(ed_no)
                        ON DELETE CASCADE,
                
                    FOREIGN KEY (topic_id)
                        REFERENCES Topics(topic_id)
                        ON DELETE CASCADE
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_topic_score_student
                    ON TopicWiseScoreCard(ed_no)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_topic_score_exam
                    ON TopicWiseScoreCard(exam_id)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_topic_score_topic
                    ON TopicWiseScoreCard(topic_id)
                    """);
        }
    }

    private int calculateCorrectAnswers(
            TreeMap<String, QuestionData> questions,
            TreeMap<Integer, Integer> answers
    ) {
        int correct = 0;

        for (var answer : answers.entrySet()) {
            QuestionData question = questions.get(
                    String.valueOf(answer.getKey())
            );

            if (question != null
                    && question.option_data() != null
                    && question.option_data().option_index()
                    == answer.getValue()) {
                correct++;
            }
        }

        return correct;
    }

    private void sendAllReviewReports(
            ExamData examInfo,
            Button sourceButton
    ) {
        Objects.requireNonNull(
                examInfo,
                "Exam information cannot be null."
        );
        Objects.requireNonNull(
                sourceButton,
                "Source button cannot be null."
        );

        sourceButton.setDisable(true);
        sourceButton.setText("Preparing...");

        CompletableFuture
                .supplyAsync(() ->
                        reviewSender.sendAll(
                                examInfo,
                                (processed, total) ->
                                        Platform.runLater(() -> {
                                            int percentage = total == 0
                                                    ? 100
                                                    : (int) Math.round(
                                                    processed * 100.0 / total
                                            );

                                            sourceButton.setText(
                                                    "Sending "
                                                            + processed
                                                            + "/"
                                                            + total
                                                            + " ("
                                                            + percentage
                                                            + "%)"
                                            );
                                        })
                        )
                )
                .whenComplete((summary, throwable) ->
                        Platform.runLater(() -> {
                            sourceButton.setDisable(false);
                            sourceButton.setText("Send Review PDFs");

                            if (throwable != null) {
                                showResultAlert(
                                        Alert.AlertType.ERROR,
                                        "Sending Failed",
                                        "Unable to send the review PDFs.",
                                        getThrowableMessage(throwable)
                                );
                                return;
                            }

                            if (summary == null) {
                                showResultAlert(
                                        Alert.AlertType.ERROR,
                                        "Sending Failed",
                                        "No sending result was returned.",
                                        "The operation completed without a summary."
                                );
                                return;
                            }

                            Alert.AlertType alertType =
                                    summary.failed() > 0
                                            || summary.missingPdf() > 0
                                            || summary.invalidTelegramId() > 0
                                            || summary.missingTelegramId() > 0
                                            ? Alert.AlertType.WARNING
                                            : Alert.AlertType.INFORMATION;

                            String header = summary.interrupted()
                                    ? "Review PDF sending was interrupted"
                                    : "Review PDF sending completed";

                            showResultAlert(
                                    alertType,
                                    "Review PDF Sharing",
                                    header,
                                    summary.toDisplayText()
                            );
                        })
                );
    }

    private void generateDiagnosticResultAsync(
            ExamData exam,
            Button button
    ) {
        button.setDisable(true);
        button.setText("Generating...");

        CompletableFuture
                .supplyAsync(() -> generateDiagnosticResult(exam))
                .whenComplete((summary, error) ->
                        Platform.runLater(() -> {
                            button.setDisable(false);
                            button.setText("Generate Result");

                            if (error != null) {
                                showResultAlert(
                                        Alert.AlertType.ERROR,
                                        "Diagnostic Result Failed",
                                        "Unable to generate diagnostic results.",
                                        getThrowableMessage(error)
                                );
                            } else {
                                showResultAlert(
                                        Alert.AlertType.INFORMATION,
                                        "Diagnostic Result Generated",
                                        "Diagnostic results generated successfully.",
                                        summary
                                );
                            }
                        })
                );
    }

    private String generateDiagnosticResult(ExamData examInfo) {
        return "Exam ID: "
                + examInfo.id()
                + "\nDiagnostic generation is handled by the existing diagnostic workflow.";
    }

    private void generateExamPdf(
            ExamData exam,
            Button button
    ) {
        button.setDisable(true);
        button.setText("Loading...");

        CompletableFuture
                .supplyAsync(() ->
                        new ArrayList<>(loadQuestion(exam).values()))
                .whenComplete((questions, error) ->
                        Platform.runLater(() -> {
                            button.setDisable(false);
                            button.setText("Exam Pdf");

                            if (error != null) {
                                showPdfError(
                                        "Unable to load exam questions.",
                                        error
                                );
                                return;
                            }

                            try {
                                Window owner = scheduleTable
                                        .getScene()
                                        .getWindow();

                                File file = ExamPdfGenerator
                                        .chooseLocationAndGenerate(
                                                owner,
                                                exam,
                                                questions
                                        );

                                if (file != null) {
                                    Alert alert = new Alert(
                                            Alert.AlertType.INFORMATION,
                                            file.getAbsolutePath(),
                                            ButtonType.OK
                                    );
                                    alert.setTitle("Exam PDF");
                                    alert.setHeaderText(
                                            "Exam PDF generated successfully"
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

    private TreeMap<String, QuestionData> loadQuestion(
            ExamData examData
    ) {
        TreeMap<String, QuestionData> questions = new TreeMap<>(
                Comparator.comparingInt(Integer::parseInt)
        );

        Connection connection = mainController
                .gradedDataLoader
                .databaseLoader
                .getConnection();

        String questionSql = """
                SELECT q.* FROM ExamQuestion eq
                INNER JOIN Questions q ON q.question_id=eq.question_id
                WHERE eq.exam_id=? ORDER BY q.question_id
                """;

        String optionSql = """
                SELECT option_id, option_text, option_order, is_correct
                FROM QuestionOptions
                WHERE question_id=?
                ORDER BY option_order
                """;

        try (PreparedStatement questionStatement =
                     connection.prepareStatement(questionSql)) {

            questionStatement.setInt(1, parseExamId(examData));

            try (ResultSet questionResult =
                         questionStatement.executeQuery()) {

                while (questionResult.next()) {
                    String questionId = questionResult.getString(
                            "question_id"
                    );
                    LinkedHashMap<Integer, String> options =
                            new LinkedHashMap<>();
                    int correctOption = 0;

                    try (PreparedStatement optionStatement =
                                 connection.prepareStatement(optionSql)) {

                        optionStatement.setString(1, questionId);

                        try (ResultSet optionResult =
                                     optionStatement.executeQuery()) {

                            while (optionResult.next()) {
                                int order = optionResult.getInt(
                                        "option_order"
                                );
                                options.put(
                                        order,
                                        optionResult.getString("option_text")
                                );

                                if (optionResult.getInt("is_correct") == 1) {
                                    correctOption = order;
                                }
                            }
                        }
                    }

                    OptionData optionData = new OptionData(
                            correctOption,
                            options
                    );

                    questions.put(
                            questionId,
                            new QuestionData(
                                    questionId,
                                    questionResult.getString("topic_id"),
                                    questionResult.getString("user_id"),
                                    questionResult.getString("date_of_making"),
                                    questionResult.getString("type"),
                                    questionResult.getString("level"),
                                    questionResult.getString("question_txt"),
                                    questionResult.getString("question_img_path"),
                                    optionData
                            )
                    );
                }
            }

            return questions;

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Unable to load exam questions.",
                    exception
            );
        }
    }

    private TreeMap<Integer, String> loadTopicNames(
            Connection connection,
            TreeMap<String, QuestionData> questions
    ) throws SQLException {

        TreeMap<Integer, String> topicNames = new TreeMap<>();

        String sql = """
                SELECT topic_id, topic_name
                FROM Topics
                WHERE topic_id = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (QuestionData question : questions.values()) {
                int topicId = parseTopicId(question);

                if (topicNames.containsKey(topicId)) {
                    continue;
                }

                statement.setInt(1, topicId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String topicName = resultSet.getString(
                                "topic_name"
                        );

                        topicNames.put(
                                topicId,
                                topicName == null || topicName.isBlank()
                                        ? "Topic " + topicId
                                        : topicName.trim()
                        );
                    } else {
                        topicNames.put(
                                topicId,
                                "Topic " + topicId
                        );
                    }
                }
            }
        }

        return topicNames;
    }

    private int parseTopicId(QuestionData question) {
        if (question == null) {
            throw new IllegalArgumentException(
                    "Question cannot be null."
            );
        }

        String topicIdValue = question.topic_id();

        if (topicIdValue == null || topicIdValue.isBlank()) {
            throw new IllegalStateException(
                    "Question "
                            + question.question_id()
                            + " does not have a topic ID."
            );
        }

        try {
            int topicId = Integer.parseInt(topicIdValue.trim());

            if (topicId <= 0) {
                throw new IllegalStateException(
                        "Question "
                                + question.question_id()
                                + " has an invalid topic ID: "
                                + topicIdValue
                );
            }

            return topicId;

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Question "
                            + question.question_id()
                            + " has a non-numeric topic ID: "
                            + topicIdValue,
                    exception
            );
        }
    }

    private List<TopicScore> calculateTopicWiseScores(
            TreeMap<String, QuestionData> questions,
            TreeMap<Integer, Integer> answers,
            TreeMap<Integer, String> topicNames,
            int marksPerQuestion
    ) {
        class MutableTopicScore {
            private final int topicId;
            private final String topicName;

            private int totalQuestions;
            private int attemptedQuestions;
            private int correctAnswers;
            private int wrongAnswers;

            private MutableTopicScore(
                    int topicId,
                    String topicName
            ) {
                this.topicId = topicId;
                this.topicName = topicName;
            }
        }

        TreeMap<Integer, MutableTopicScore> groupedScores =
                new TreeMap<>();

        /*
         * Iterate over every exam question.
         *
         * Do not iterate only over answers, because unanswered questions
         * must still be counted in totalQuestions and totalMarks.
         */
        for (QuestionData question : questions.values()) {
            int topicId = parseTopicId(question);

            String topicName = topicNames.getOrDefault(
                    topicId,
                    "Topic " + topicId
            );

            MutableTopicScore topicScore =
                    groupedScores.computeIfAbsent(
                            topicId,
                            ignored -> new MutableTopicScore(
                                    topicId,
                                    topicName
                            )
                    );

            topicScore.totalQuestions++;

            int questionId;

            try {
                questionId = Integer.parseInt(
                        question.question_id()
                );
            } catch (NumberFormatException exception) {
                throw new IllegalStateException(
                        "Invalid question ID: "
                                + question.question_id(),
                        exception
                );
            }

            Integer selectedOption = answers.get(questionId);

            /*
             * No answer means the question is unattempted.
             */
            if (selectedOption == null) {
                continue;
            }

            topicScore.attemptedQuestions++;

            boolean isCorrect =
                    question.option_data() != null
                            && question.option_data().option_index()
                            == selectedOption;

            if (isCorrect) {
                topicScore.correctAnswers++;
            } else {
                topicScore.wrongAnswers++;
            }
        }

        List<TopicScore> topicScores = new ArrayList<>();

        for (MutableTopicScore grouped : groupedScores.values()) {
            int marksObtained =
                    grouped.correctAnswers * marksPerQuestion;

            int totalMarks =
                    grouped.totalQuestions * marksPerQuestion;

            String remark;

            if (answers.isEmpty()) {
                remark = "Absent or not eligible";
            } else if (grouped.attemptedQuestions == 0) {
                remark = "Not attempted";
            } else {
                remark = "Present";
            }

            topicScores.add(
                    new TopicScore(
                            grouped.topicId,
                            grouped.topicName,
                            grouped.totalQuestions,
                            grouped.attemptedQuestions,
                            grouped.correctAnswers,
                            grouped.wrongAnswers,
                            marksObtained,
                            totalMarks,
                            remark
                    )
            );
        }

        return topicScores;
    }

    private void validateBatchResults(
            int[] results,
            String tableName
    ) throws SQLException {

        for (int result : results) {
            if (result == Statement.EXECUTE_FAILED) {
                throw new SQLException(
                        "One or more "
                                + tableName
                                + " records failed."
                );
            }
        }
    }

    public static TreeMap<Integer, Integer> getAnswers(
            int examId,
            String studentEd
    ) {
        TreeMap<Integer, Integer> answers = new TreeMap<>();

        if (examId <= 0
                || studentEd == null
                || studentEd.isBlank()) {
            return answers;
        }

        File database = new File(
                Main.getRootPath(),
                "GradeEd_Exam_2026/" + studentEd.trim() + ".db"
        );

        if (!database.isFile()) {
            return answers;
        }

        String sql = """
                SELECT question_id, selected_option_id
                FROM answers
                WHERE exam_id=?
                ORDER BY question_id
                """;

        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + database.getAbsolutePath()
        ); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, examId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Integer selectedOption = result.getObject(
                            "selected_option_id",
                            Integer.class
                    );
                    int questionId = result.getInt("question_id");

                    if (questionId > 0
                            && selectedOption != null
                            && selectedOption >= 1
                            && selectedOption <= 4) {
                        answers.put(questionId, selectedOption);
                    }
                }
            }

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Could not read answers for " + studentEd,
                    exception
            );
        }

        return answers;
    }

    public void saveFromOMR(
            String studentEd,
            List<String> selectedOptions,
            TreeMap<String, QuestionData> questions,
            ExamData exam
    ) {
        File directory = new File(
                Main.getRootPath(),
                "GradeEd_Exam_2026"
        );

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException(
                    "Cannot create exam database directory."
            );
        }

        File studentDatabase = new File(
                directory,
                studentEd + ".db"
        );

        String createTableSql = """
                CREATE TABLE IF NOT EXISTS answers (
                    exam_id INTEGER NOT NULL,
                    question_id INTEGER NOT NULL,
                    selected_option_id INTEGER,
                    time_slot TEXT,
                    start_time TEXT,
                    end_time TEXT,
                    created_at TEXT,
                    PRIMARY KEY (exam_id, question_id)
                )
                """;

        String insertSql = """
                INSERT INTO answers
                (exam_id, question_id, selected_option_id, time_slot, start_time, end_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(exam_id, question_id) DO UPDATE SET
                  selected_option_id=excluded.selected_option_id,
                  time_slot=excluded.time_slot,
                  start_time=excluded.start_time,
                  end_time=excluded.end_time,
                  created_at=excluded.created_at
                """;

        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + studentDatabase.getAbsolutePath()
        )) {
            connection.setAutoCommit(false);

            try (Statement createStatement = connection.createStatement()) {
                createStatement.execute(createTableSql);
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(insertSql)) {

                int index = 0;

                for (QuestionData question : questions.values()) {
                    if (index >= selectedOptions.size()) {
                        break;
                    }

                    int selectedOption = convertToNumeric(
                            selectedOptions.get(index++)
                    );

                    statement.setInt(1, parseExamId(exam));
                    statement.setInt(
                            2,
                            Integer.parseInt(question.question_id())
                    );

                    if (selectedOption == 0) {
                        statement.setNull(3, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(3, selectedOption);
                    }

                    statement.setString(4, "");
                    statement.setString(5, "");
                    statement.setString(6, LocalTime.now().toString());
                    statement.setString(7, LocalDate.now().toString());
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Unable to import OMR answers for " + studentEd,
                    exception
            );
        }
    }

    private int convertToNumeric(String value) {
        return switch (value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT)) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 0;
        };
    }

    private int parseExamId(ExamData exam) {
        try {
            return Integer.parseInt(exam.id());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid exam ID: " + exam.id(),
                    exception
            );
        }
    }

    private boolean containsMoreThanOneSubject(String value) {
        return value != null
                && value.contains("[")
                && value.contains("]");
    }

    private String format(String value) {
        return value.charAt(0)
                + value.substring(1).toLowerCase(Locale.ROOT);
    }

    public File folderChooser() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("OMR Image folder Selector");
        return chooser.showDialog(mainController.getStage());
    }

    private void openPdfDirectory(File directory) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new UnsupportedOperationException(
                        "Desktop operations are not supported."
                );
            }
            Desktop.getDesktop().open(directory);
        } catch (Exception exception) {
            showResultAlert(
                    Alert.AlertType.ERROR,
                    "PDF Directory",
                    "Unable to open the PDF directory.",
                    getThrowableMessage(exception)
            );
        }
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
        initializeOwner(alert);
        alert.show();
    }

    private void initializeOwner(Alert alert) {
        if (scheduleTable != null
                && scheduleTable.getScene() != null
                && scheduleTable.getScene().getWindow() != null) {
            alert.initOwner(scheduleTable.getScene().getWindow());
        }
    }

    private void showPdfError(
            String message,
            Throwable throwable
    ) {
        if (throwable != null) {
            throwable.printStackTrace();
        }

        showResultAlert(
                Alert.AlertType.ERROR,
                "Exam PDF Error",
                message,
                throwable == null
                        ? ""
                        : getThrowableMessage(throwable)
        );
    }

    private String getThrowableMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage() == null
                || cause.getMessage().isBlank()
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }

    private record TopicScore(
            int topicId,
            String topicName,
            int totalQuestions,
            int attemptedQuestions,
            int correctAnswers,
            int wrongAnswers,
            int marksObtained,
            int totalMarks,
            String remark
    ) {
    }
}
