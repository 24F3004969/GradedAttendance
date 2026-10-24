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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.components.LatexImageRenderer;
import org.graded_classes.graded_attendance.components.OMRParser;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.ExamData;
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConductExam implements Initializable {
    private static final PDRectangle REVIEW_PAGE_SIZE = PDRectangle.A4;
    private static final float REVIEW_MARGIN = 45f;
    private static final float REVIEW_TOP_MARGIN = 48f;
    private static final float REVIEW_BOTTOM_MARGIN = 45f;
    private static final float REVIEW_CONTENT_WIDTH = REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN * 2f;
    private static final float REVIEW_NORMAL_FONT_SIZE = 10f;
    private static final float REVIEW_SMALL_FONT_SIZE = 8f;
    private static final float REVIEW_HEADING_FONT_SIZE = 16f;
    private static final float REVIEW_LINE_HEIGHT = 14f;
    private static final float REVIEW_OPTION_LINE_HEIGHT = 13f;

    private final MainController mainController;
    private final ArrayList<ExamData> examSchedular;

    @FXML private TableColumn<ExamData, HBox> action;
    @FXML private TableColumn<ExamData, Button> generate;
    @FXML private TableColumn<ExamData, Button> add;
    @FXML private TableColumn<ExamData, String> classes;
    @FXML private TableColumn<ExamData, String> doe;
    @FXML private TableColumn<ExamData, String> id;
    @FXML private TableColumn<ExamData, String> room;
    @FXML private TableColumn<ExamData, String> subject;
    @FXML private TableColumn<ExamData, String> time;
    @FXML private TableColumn<ExamData, String> topic;
    @FXML private TableView<ExamData> scheduleTable;
    @FXML private Label today;
    @FXML
    private TableColumn<ExamData, Button> share;

     final ObservableList<ExamData> items = FXCollections.observableArrayList();

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
        LocalDate now = LocalDate.now();
        today.setText(now.getDayOfMonth() + " " + format(now.getMonth().toString()) + " " + now.getYear());
        classes.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().classes()));
        doe.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().doe()));
        id.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().id()));
        room.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().room()));
        subject.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().subject()));
        time.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().time()));
        topic.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().topicNamesForDisplay()));

        action.setCellValueFactory(cell -> {
            ExamData exam = cell.getValue();
            Button start = new Button("Start Exam");
            Button result = new Button("Generate Result");
            start.setPadding(new Insets(5));
            result.setPadding(new Insets(5));
            start.getStyleClass().add(Styles.SUCCESS);
            result.getStyleClass().add(Styles.SUCCESS);
            start.setOnAction(e -> mainController.modalPane.show(mainController.gradedFxmlLoader.createView(
                    R.exam_entry_login,
                    new LoginBeforeEntry(mainController, exam, containsMoreThanOneSubject(exam.subject()) ? "Diagnostic" : "Normal")
            )));
            result.setOnAction(e -> {
                if (containsMoreThanOneSubject(exam.subject())) {
                    generateDiagnosticResultAsync(exam, result);
                    Node node = mainController.gradedFxmlLoader.createView(
                            R.diagnostic_exam_report, new DiagnosticExamReport(mainController));
                    node.setStyle("-fx-background-color: #fafafa");
                    if (node instanceof Region region) {
                        region.setPrefSize(900, 700);
                        region.setMaxSize(900, 700);
                    }
                    mainController.modalPane.show(node);
                } else {
                    generateNormalResultAsync(exam, result);
                }
            });
            HBox box = new HBox(5, start, result);
            box.setAlignment(Pos.CENTER);
            return new SimpleObjectProperty<>(box);
        });
        share.setCellValueFactory(cell -> {
            ExamData examInfo = cell.getValue();

            Button shareButton =
                    new Button("Send Review PDFs");

            shareButton.getStyleClass().add(
                    Styles.ACCENT
            );

            shareButton.setPadding(
                    new Insets(8)
            );

            shareButton.setMaxWidth(
                    Double.MAX_VALUE
            );

            shareButton.setOnAction(event ->
                    sendAllReviewReports(
                            examInfo,
                            shareButton
                    )
            );

            return new SimpleObjectProperty<>(
                    shareButton
            );
        });
        add.setCellValueFactory(cell -> {
            ExamData exam = cell.getValue();
            Button button = new Button("Add exam OMR");
            button.setOnAction(e -> importOmr(exam));
            return new SimpleObjectProperty<>(button);
        });

        generate.setCellValueFactory(cell -> {
            ExamData exam = cell.getValue();
            Button button = new Button("Exam Pdf");
            button.getStyleClass().add(Styles.ACCENT);
            button.setPadding(new Insets(8));
            button.setOnAction(e -> generateExamPdf(exam, button));
            return new SimpleObjectProperty<>(button);
        });

        items.setAll(examSchedular);
        scheduleTable.setItems(items);
    }

    private void importOmr(ExamData examInfo) {
        File generatedFile = fileChooser();
        if (generatedFile == null) return;
        TreeMap<String, QuestionData> questionMap = loadQuestion(examInfo);
        var omrData = OMRParser.parse(generatedFile.getAbsolutePath());
        List<String> missing = new ArrayList<>();
        for (var entry : omrData.entrySet()) {
            String roll = entry.getKey();
            if (!mainController.gradedDataLoader.getStudentData().containsKey(roll)) {
                missing.add(roll);
                continue;
            }
            saveFromOMR(roll, entry.getValue().values().stream().toList(), questionMap, examInfo);
        }
        if (!missing.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Student Not Found");
            alert.setHeaderText(missing.size() + " student(s) not found");
            alert.setContentText(String.join(", ", missing));
            alert.show();
        }
    }

    private void generateNormalResultAsync(ExamData examInfo, Button sourceButton) {
        sourceButton.setDisable(true);
        sourceButton.setText("Generating...");
        CompletableFuture.supplyAsync(() -> generateResult(examInfo)).whenComplete((directory, throwable) ->
                Platform.runLater(() -> {
                    sourceButton.setDisable(false);
                    sourceButton.setText("Generate Result");
                    if (throwable != null) {
                        showResultAlert(Alert.AlertType.ERROR, "Result Generation Failed",
                                "Unable to generate the result PDFs.", getThrowableMessage(throwable));
                        return;
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Result Generated");
                    alert.setHeaderText("Results and answer-review PDFs generated successfully.");
                    alert.setContentText("The ScoreCard table was updated.\n\nPDF directory:\n" + directory.getAbsolutePath());
                    ButtonType open = new ButtonType("Open PDF Folder", ButtonBar.ButtonData.LEFT);
                    alert.getButtonTypes().setAll(open, ButtonType.OK);
                    if (scheduleTable.getScene() != null) alert.initOwner(scheduleTable.getScene().getWindow());
                    alert.showAndWait().filter(open::equals).ifPresent(b -> openPdfDirectory(directory));
                }));
    }

    private File generateResult(ExamData examInfo) {
        int examId = Integer.parseInt(examInfo.id());
        File reviewDirectory = new File(Main.getRootPath(), "ExamAnswerReviews/Exam_" + examId);
        if (!reviewDirectory.exists() && !reviewDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create PDF directory: " + reviewDirectory.getAbsolutePath());
        }
        TreeMap<String, QuestionData> questions = loadQuestion(examInfo);
        if (questions.isEmpty()) throw new IllegalStateException("No questions found for exam " + examId);
        int marksPerQuestion = 4;
        int totalMarks = questions.size() * marksPerQuestion;
        var students = mainController.gradedDataLoader.getStudentData().sequencedValues().stream()
                .filter(s -> Objects.equals(s._class(), examInfo.classes()))
                .filter(s -> s.getBoard() != null && examInfo.board() != null
                        && (s.getBoard().equalsIgnoreCase(examInfo.board()) || examInfo.board().equalsIgnoreCase("Both")))
                .toList();
        String sql = """
                INSERT INTO ScoreCard
                (exam_id, ed_no, subject, topic_name, marks_obtain, total_marks, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(exam_id, ed_no) DO UPDATE SET
                  subject=excluded.subject,
                  topic_name=excluded.topic_name,
                  marks_obtain=excluded.marks_obtain,
                  total_marks=excluded.total_marks,
                  remark=excluded.remark
                """;
        Connection con = mainController.gradedDataLoader.databaseLoader.getConnection();
        boolean oldAutoCommit;
        try {
            oldAutoCommit = con.getAutoCommit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (var student : students) {
                    TreeMap<Integer, Integer> answers = getAnswers(examId, student.ed_no());
                    int correct = 0;
                    for (var answer : answers.entrySet()) {
                        QuestionData q = questions.get(String.valueOf(answer.getKey()));
                        if (q != null && q.option_data() != null
                                && q.option_data().option_index() == answer.getValue()) correct++;
                    }
                    int score = correct * marksPerQuestion;
                    String remark = answers.isEmpty() ? "Absent or not eligible" : "Present";
                    generateStudentAnswerReviewPdf(examInfo, student.ed_no(), student.name(), questions, answers);
                    ps.setInt(1, examId);
                    ps.setString(2, student.ed_no());
                    ps.setString(3, examInfo.subject());
                    ps.setString(4, examInfo.topicNamesForDisplay());
                    ps.setInt(5, score);
                    ps.setInt(6, totalMarks);
                    ps.setString(7, remark);
                    ps.addBatch();
                }
                for (int result : ps.executeBatch()) {
                    if (result == Statement.EXECUTE_FAILED) throw new SQLException("One or more ScoreCard records failed.");
                }
            }
            con.commit();
            return reviewDirectory;
        } catch (Exception e) {
            try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
            throw new RuntimeException("Failed to generate results for exam ID: " + examId, e);
        } finally {
            try { con.setAutoCommit(oldAutoCommit); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private record AnswerReviewPdfResult(String studentEd, String studentName, int obtainedMarks,
                                         int totalMarks, int attempted, int correct, int wrong,
                                         int unanswered, File pdfFile) {}

    private AnswerReviewPdfResult generateStudentAnswerReviewPdf(
            ExamData examInfo, String studentEd, String studentName,
            TreeMap<String, QuestionData> questionMap, Map<Integer, Integer> submittedAnswers) {
        String safeName = studentName == null || studentName.isBlank() ? studentEd : studentName.trim();
        int correct = 0;
        int attempted = 0;
        for (QuestionData q : questionMap.values()) {
            Integer selected = submittedAnswers.get(Integer.parseInt(q.question_id()));
            if (selected != null) {
                attempted++;
                if (q.option_data() != null && selected == q.option_data().option_index()) correct++;
            }
        }
        int wrong = attempted - correct;
        int unanswered = questionMap.size() - attempted;
        int obtained = correct * 4;
        int total = questionMap.size() * 4;
        Path directory = Path.of(Main.getRootPath(), "ExamAnswerReviews", "Exam_" + examInfo.id());
        try { Files.createDirectories(directory); } catch (IOException e) { throw new RuntimeException(e); }
        File output = directory.resolve(sanitizePdfFileName(studentEd + "_" + safeName + "_Answer_Review.pdf")).toFile();

        try (PDDocument document = new PDDocument();
             InputStream normalStream = getClass().getResourceAsStream("/org/graded_classes/graded_attendance/fonts/NotoSans-Regular.ttf");
             InputStream boldStream = getClass().getResourceAsStream("/org/graded_classes/graded_attendance/fonts/NotoSans-Bold.ttf")) {
            if (normalStream == null || boldStream == null) throw new IllegalStateException("NotoSans PDF fonts were not found.");
            PDFont normal = PDType0Font.load(document, normalStream, true);
            PDFont bold = PDType0Font.load(document, boldStream, true);
            try (PdfReviewContext context = new PdfReviewContext(document, normal, bold)) {
                writeReviewHeader(context, examInfo, studentEd, safeName, obtained, total, attempted, correct, wrong, unanswered);
                int number = 1;
                for (QuestionData question : questionMap.values()) {
                    writeQuestionReview(context, document, number++, question, submittedAnswers);
                }
                writePdfFooterNote(context);
            }
            document.save(output);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate answer-review PDF for " + safeName, e);
        }
        return new AnswerReviewPdfResult(studentEd, safeName, obtained, total, attempted, correct, wrong, unanswered, output);
    }

    private void writeReviewHeader(PdfReviewContext c, ExamData exam, String ed, String name,
                                   int obtained, int total, int attempted, int correct, int wrong, int unanswered) throws IOException {
        c.ensureSpace(190f);
        writePdfText(c, "EXAM ANSWER REVIEW", REVIEW_MARGIN, c.y(), REVIEW_HEADING_FONT_SIZE, c.boldFont(), new Color(18, 52, 91));
        c.moveDown(25);
        writePdfText(c, "Student: " + nullSafePdfText(name), REVIEW_MARGIN, c.y(), 11, c.boldFont(), Color.BLACK);
        c.moveDown(17);
        writePdfText(c, "Enrollment No.: " + nullSafePdfText(ed), REVIEW_MARGIN, c.y(), 10, c.normalFont(), Color.DARK_GRAY);
        c.moveDown(15);
        writePdfText(c, "Exam ID: " + nullSafePdfText(exam.id()), REVIEW_MARGIN, c.y(), 10, c.normalFont(), Color.DARK_GRAY);
        c.moveDown(15);
        writePdfText(c, "Subject: " + nullSafePdfText(exam.subject()), REVIEW_MARGIN, c.y(), 10, c.normalFont(), Color.DARK_GRAY);
        c.moveDown(15);
        writeWrappedPdfText(c, "Topics: " + nullSafePdfText(exam.topicNamesForDisplay()), REVIEW_MARGIN,
                REVIEW_CONTENT_WIDTH, 10, REVIEW_LINE_HEIGHT, c.normalFont(), Color.DARK_GRAY);
        c.moveDown(4);
        drawPdfLine(c, REVIEW_MARGIN, c.y(), REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN, c.y(), new Color(200,210,220), .8f);
        c.moveDown(18);
        double percentage = total == 0 ? 0 : obtained * 100.0 / total;
        writePdfText(c, String.format(Locale.ROOT, "Marks: %d/%d     Percentage: %.2f%%", obtained, total, percentage),
                REVIEW_MARGIN, c.y(), 12, c.boldFont(), new Color(18,52,91));
        c.moveDown(20);
        writePdfText(c, "Attempted: " + attempted + "     Correct: " + correct + "     Wrong: " + wrong
                + "     Unanswered: " + unanswered, REVIEW_MARGIN, c.y(), 10, c.boldFont(), Color.DARK_GRAY);
        c.moveDown(25);
    }

    private void writeQuestionReview(PdfReviewContext c, PDDocument document, int number,
                                     QuestionData question, Map<Integer, Integer> answers) throws IOException {
        int questionId = Integer.parseInt(question.question_id());
        Integer selected = answers.get(questionId);
        int correctIndex = question.option_data() == null ? 0 : question.option_data().option_index();
        boolean attempted = selected != null && selected >= 1 && selected <= 4;
        boolean correct = attempted && selected == correctIndex;
        Color statusColor = !attempted ? new Color(217,119,6) : correct ? new Color(22,163,74) : new Color(220,38,38);
        String statusText = !attempted ? "NOT ANSWERED" : correct ? "CORRECT" : "INCORRECT";

        c.ensureSpace(165f);
        drawPdfLine(c, REVIEW_MARGIN, c.y(), REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN, c.y(), statusColor, 2);
        c.moveDown(17);
        writePdfText(c, "Question " + number + "  [ID: " + questionId + "]", REVIEW_MARGIN, c.y(), 12,
                c.boldFont(), new Color(18,52,91));
        writeRightAlignedPdfText(c, statusText, REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN, c.y(), 9,
                c.boldFont(), statusColor);
        c.moveDown(22);

        PreparedQuestion preparedQuestion = classifyQuestionContent(question.question_txt());
        if (preparedQuestion.plainText()) {
            writePlainQuestionText(c, preparedQuestion.content());
        } else {
            float questionHeight = drawLatexOnPdf(c, preparedQuestion.content(), REVIEW_MARGIN,
                    c.y(), REVIEW_CONTENT_WIDTH, 11.5f, Color.BLACK);
            c.moveDown(Math.max(questionHeight, REVIEW_LINE_HEIGHT) + 12f);
        }
        writeQuestionImageIfPresent(c, document, question.question_img_path());

        Map<Integer, String> options = question.option_data() == null || question.option_data().options() == null
                ? Collections.emptyMap() : question.option_data().options();
        for (var option : options.entrySet()) {
            int optionIndex = option.getKey();
            writeReviewOption(c, optionIndex, option.getValue(), Objects.equals(selected, optionIndex), optionIndex == correctIndex);
        }
        c.moveDown(6);
        String explanation = "Student answer: " + (attempted ? optionIndexToLetter(selected) : "Not answered")
                + "    |    Correct answer: " + optionIndexToLetter(correctIndex);
        Color explanationColor = !attempted ? new Color(146,64,14) : correct ? new Color(22,101,52) : new Color(153,27,27);
        writeWrappedPdfText(c, explanation, REVIEW_MARGIN, REVIEW_CONTENT_WIDTH, 10, REVIEW_LINE_HEIGHT,
                c.boldFont(), explanationColor);
        c.moveDown(22);
    }

    private void writeReviewOption(PdfReviewContext c, int index, String value,
                                   boolean selected, boolean correct) throws IOException {
        Color color = Color.DARK_GRAY;
        String status = "";
        if (correct && selected) { color = new Color(22,101,52); status = "[CORRECT - SELECTED]"; }
        else if (correct) { color = new Color(22,101,52); status = "[CORRECT ANSWER]"; }
        else if (selected) { color = new Color(153,27,27); status = "[SELECTED - WRONG]"; }
        c.ensureSpace(42);
        float top = c.y();
        float labelX = REVIEW_MARGIN + 14;
        float statusX = labelX + 24;
        float formulaX = REVIEW_MARGIN + 178;
        float baseline = top - 10;
        writePdfText(c, optionIndexToLetter(index) + ".", labelX, baseline, 10,
                correct || selected ? c.boldFont() : c.normalFont(), color);
        if (!status.isBlank()) writePdfText(c, status, statusX, baseline, 8.5f, c.boldFont(), color);
        float available = REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN - formulaX;
        float contentHeight;
        if (isPlainOptionText(value)) {
            writePdfText(c, sanitizePdfText(value.trim()), formulaX, baseline,
                    REVIEW_NORMAL_FONT_SIZE,
                    correct || selected ? c.boldFont() : c.normalFont(), color);
            contentHeight = REVIEW_OPTION_LINE_HEIGHT;
        } else {
            contentHeight = drawLatexOnPdf(c, prepareOptionLatex(value), formulaX, top,
                    available, REVIEW_NORMAL_FONT_SIZE, color);
        }
        c.moveDown(Math.max(contentHeight, REVIEW_OPTION_LINE_HEIGHT) + 8f);
    }

    private record PreparedQuestion(boolean plainText, String content) {}

    private PreparedQuestion classifyQuestionContent(String value) {
        if (value == null || value.isBlank()) {
            return new PreparedQuestion(true, "No question text available");
        }
        String source = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        boolean mathematical = containsUnescapedDollar(source)
                || Pattern.compile("\\\\(?:frac|dfrac|tfrac|sqrt|sin|cos|tan|cot|sec|csc|cosec|log|ln|sum|prod|int|lim|begin|end|left|right|operatorname|mathrm|alpha|beta|gamma|theta|pi|leq|geq|neq|infty|times|div|circ)\\b")
                .matcher(source).find()
                || Pattern.compile("[A-Za-z0-9)}]\\s*[\\^_]\\s*(?:\\{[^{}]+}|[^\\s,.!?]+)").matcher(source).find()
                || Pattern.compile("\\b\\d+\\s*:\\s*\\d+\\b").matcher(source).find()
                || Pattern.compile("[A-Za-z0-9)}]\\s*[=+*/]\\s*[A-Za-z0-9({\\\\]").matcher(source).find();
        if (!mathematical) {
            return new PreparedQuestion(true,
                    source.replace("\\\\", "\n").replaceAll("[ ]+", " ").trim());
        }
        return new PreparedQuestion(false, prepareQuestionLatex(source));
    }

    private void writePlainQuestionText(PdfReviewContext c, String text) throws IOException {
        String normalized = text == null || text.isBlank()
                ? "No question text available"
                : text.replace("\r\n", "\n").replace('\r', '\n').replace("\\\\", "\n");
        boolean written = false;
        for (String paragraph : normalized.split("\n", -1)) {
            String clean = paragraph.trim().replaceAll("\\s+", " ");
            if (clean.isBlank()) {
                if (written) c.moveDown(5f);
                continue;
            }
            writeWrappedPdfText(c, clean, REVIEW_MARGIN, REVIEW_CONTENT_WIDTH,
                    11f, 15f, c.normalFont(), Color.BLACK);
            written = true;
        }
        c.moveDown(12f);
    }

    private boolean isPlainOptionText(String value) {
        return value == null || value.isBlank()
                || (!looksLikeLatex(value)
                && !looksLikeMathematicalOption(value)
                && !containsUnescapedDollar(value));
    }

    private String prepareQuestionLatex(String value) {
        if (value == null || value.isBlank()) return "\\text{No question text available}";
        String source = normalizeLatexSource(value);
        if (containsUnescapedDollar(source)) return convertDollarMixedContent(source);
        source = removeLatexDelimiters(source);
        int first = findFirstLatexCommand(source);
        if (first > 0) {
            String words = source.substring(0, first).trim();
            String formula = normalizeLatexForJLatexMath(source.substring(first).trim());
            return "\\begin{array}{l}\\text{" + escapeLatexText(words) + "}\\\\" + formula + "\\end{array}";
        }
        if (first == 0 || looksLikeLatex(source)) return normalizeLatexForJLatexMath(source);
        int mathWord = findFirstMathematicalWord(source);
        if (mathWord > 0) {
            String words = source.substring(0, mathWord).trim();
            String formula = normalizeMathWords(source.substring(mathWord).trim());
            return "\\begin{array}{l}\\text{" + escapeLatexText(words) + "}\\\\" + formula + "\\end{array}";
        }
        if (looksLikeMathematicalOption(source)) return normalizeMathWords(source);
        return "\\text{" + escapeLatexText(source) + "}";
    }

    private String prepareOptionLatex(String value) {
        if (value == null || value.isBlank()) return "\\text{}";
        String source = normalizeLatexSource(value);
        if (containsUnescapedDollar(source)) return convertDollarMixedContent(source);
        source = removeLatexDelimiters(source);
        if (looksLikeLatex(source)) return normalizeLatexForJLatexMath(source);
        if (looksLikeMathematicalOption(source)) return normalizeMathWords(source);
        return "\\text{" + escapeLatexText(source) + "}";
    }

    private String normalizeLatexSource(String value) {
        return value == null ? "" : value.replace('\r',' ').replace('\n',' ').replace('\t',' ').trim().replaceAll("\\s+", " ");
    }

    private int findFirstLatexCommand(String value) {
        String[] commands = {"\\frac","\\dfrac","\\tfrac","\\sqrt","\\begin","\\left","\\sin","\\cos",
                "\\tan","\\cot","\\sec","\\csc","\\operatorname","\\mathrm","\\sum","\\prod","\\int",
                "\\lim","\\vec","\\overline","\\underline","\\angle"};
        int earliest = -1;
        for (String command : commands) {
            int p = value.indexOf(command);
            if (p >= 0 && (earliest < 0 || p < earliest)) earliest = p;
        }
        return earliest;
    }

    private int findFirstMathematicalWord(String value) {
        Matcher m = Pattern.compile("(?i)\\b(cosec|csc|sec|cot|tan|sin|cos|log|ln)\\b").matcher(value);
        return m.find() ? m.start() : -1;
    }

    private boolean looksLikeLatex(String value) {
        return value != null && (value.contains("\\") || value.contains("^{") || value.contains("_{"));
    }

    private boolean looksLikeMathematicalOption(String value) {
        if (value == null || value.isBlank()) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.matches(".*\\b(sin|cos|tan|cot|sec|cosec|csc|log|ln)\\b.*")
                || value.matches(".*[=+\\-*/×÷^_].*") || value.matches(".*\\d.*")
                || value.contains("(") || value.contains(")");
    }

    private String normalizeMathWords(String value) {
        String result = normalizeLatexSource(value);
        result = result.replaceAll("(?i)\\bcosec\\s*", "\\\\mathrm{cosec}\\\\,");
        result = result.replaceAll("(?i)\\bcsc\\s*", "\\\\csc\\\\,");
        result = result.replaceAll("(?i)\\bsec\\s*", "\\\\sec\\\\,");
        result = result.replaceAll("(?i)\\bcot\\s*", "\\\\cot\\\\,");
        result = result.replaceAll("(?i)\\btan\\s*", "\\\\tan\\\\,");
        result = result.replaceAll("(?i)\\bsin\\s*", "\\\\sin\\\\,");
        result = result.replaceAll("(?i)\\bcos\\s*", "\\\\cos\\\\,");
        result = result.replaceAll("(?i)\\blog\\s*", "\\\\log\\\\,");
        result = result.replaceAll("(?i)\\bln\\s*", "\\\\ln\\\\,");
        return normalizeLatexForJLatexMath(result);
    }

    private String normalizeLatexForJLatexMath(String latex) {
        if (latex == null || latex.isBlank()) return "\\text{}";
        String s = removeLatexDelimiters(latex.trim());
        s = s.replaceAll("\\\\operatorname\\s*\\{([^{}]*)}", "\\\\mathrm{$1}");
        s = s.replaceAll("\\\\arccosec(?![A-Za-z])", "\\\\mathrm{arccosec}");
        s = s.replaceAll("\\\\arcsec(?![A-Za-z])", "\\\\mathrm{arcsec}");
        s = s.replaceAll("\\\\cosec(?![A-Za-z])", "\\\\mathrm{cosec}");
        return s.replace("×", "\\times ").replace("÷", "\\div ").replace("−", "-")
                .replace("–", "-").replace("≤", "\\leq ").replace("≥", "\\geq ")
                .replace("≠", "\\neq ").replace("∞", "\\infty ").replace("√", "\\sqrt ").trim();
    }

    private boolean containsUnescapedDollar(String value) {
        for (int i = 0; value != null && i < value.length(); i++)
            if (value.charAt(i) == '$' && !isEscapedCharacter(value, i)) return true;
        return false;
    }

    private boolean isEscapedCharacter(String value, int index) {
        int count = 0;
        for (int i = index - 1; i >= 0 && value.charAt(i) == '\\'; i--) count++;
        return count % 2 != 0;
    }

    private String convertDollarMixedContent(String source) {
        StringBuilder out = new StringBuilder(), part = new StringBuilder();
        boolean formula = false;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '$' && !isEscapedCharacter(source, i)) {
                appendMixedLatexSection(out, part.toString(), formula);
                part.setLength(0);
                formula = !formula;
            } else part.append(ch);
        }
        appendMixedLatexSection(out, part.toString(), formula);
        return out.toString().trim();
    }

    private void appendMixedLatexSection(StringBuilder output, String section, boolean formula) {
        if (section == null || section.isEmpty()) return;
        if (formula) output.append(normalizeLatexForJLatexMath(section.trim()));
        else if (section.isBlank()) output.append("\\;");
        else output.append("\\text{").append(escapeLatexText(section)).append("}");
    }

    private String escapeLatexText(String text) {
        StringBuilder result = new StringBuilder();
        if (text == null) return "";
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\' -> result.append("\\backslash ");
                case '{' -> result.append("\\{");
                case '}' -> result.append("\\}");
                case '%' -> result.append("\\%");
                case '#' -> result.append("\\#");
                case '&' -> result.append("\\&");
                case '_' -> result.append("\\_");
                case '$' -> result.append("\\$");
                case '^' -> result.append("\\hat{}");
                default -> result.append(c);
            }
        }
        return result.toString();
    }

    private String removeLatexDelimiters(String text) {
        if (text == null) return "";
        String s = text.trim();
        if (s.length() >= 4 && s.startsWith("$$") && s.endsWith("$$")) return s.substring(2, s.length()-2).trim();
        if (s.length() >= 2 && s.startsWith("$") && s.endsWith("$")) return s.substring(1, s.length()-1).trim();
        if (s.length() >= 4 && s.startsWith("\\(") && s.endsWith("\\)")) return s.substring(2, s.length()-2).trim();
        if (s.length() >= 4 && s.startsWith("\\[") && s.endsWith("\\]")) return s.substring(2, s.length()-2).trim();
        return s;
    }

    private float drawLatexOnPdf(PdfReviewContext c, String formula, float x, float topY,
                                 float maxWidth, float fontSize, Color color) throws IOException {
        BufferedImage image = LatexImageRenderer.render(normalizeLatexForJLatexMath(formula), fontSize, color, true);
        float quality = (float) LatexImageRenderer.RENDER_QUALITY;
        float width = image.getWidth() / quality;
        float height = image.getHeight() / quality;
        if (maxWidth > 0 && width > maxWidth) {
            float scale = maxWidth / width;
            width *= scale;
            height *= scale;
        }
        float oldY = c.y();
        c.ensureSpace(height + 6);
        if (Float.compare(oldY, c.y()) != 0) topY = c.y();
        PDImageXObject pdfImage = LosslessFactory.createFromImage(c.document(), image);
        c.stream().drawImage(pdfImage, x, topY - height, width, height);
        return height;
    }

    private TreeMap<String, QuestionData> loadQuestion(ExamData examData) {
        TreeMap<String, QuestionData> map = new TreeMap<>(Comparator.comparingInt(Integer::parseInt));
        Connection connection = mainController.gradedDataLoader.databaseLoader.getConnection();
        String sql = """
                SELECT q.* FROM ExamQuestion eq
                INNER JOIN Questions q ON q.question_id=eq.question_id
                WHERE eq.exam_id=? ORDER BY q.question_id
                """;
        String optionSql = """
                SELECT option_id, option_text, option_order, is_correct
                FROM QuestionOptions WHERE question_id=? ORDER BY option_order
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(examData.id()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String qid = rs.getString("question_id");
                    LinkedHashMap<Integer, String> options = new LinkedHashMap<>();
                    int correct = 0;
                    try (PreparedStatement ops = connection.prepareStatement(optionSql)) {
                        ops.setString(1, qid);
                        try (ResultSet ors = ops.executeQuery()) {
                            while (ors.next()) {
                                int order = ors.getInt("option_order");
                                options.put(order, ors.getString("option_text"));
                                if (ors.getInt("is_correct") == 1) correct = order;
                            }
                        }
                    }
                    OptionData optionData = new OptionData(correct, options);
                    map.put(qid, new QuestionData(qid, rs.getString("topic_id"), rs.getString("user_id"),
                            rs.getString("date_of_making"), rs.getString("type"), rs.getString("level"),
                            rs.getString("question_txt"), rs.getString("question_img_path"), optionData));
                }
            }
            return map;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load exam questions.", e);
        }
    }

    public static TreeMap<Integer, Integer> getAnswers(int examId, String studentEd) {
        TreeMap<Integer, Integer> answers = new TreeMap<>();
        if (examId <= 0 || studentEd == null || studentEd.isBlank()) return answers;
        File database = new File(Main.getRootPath(), "GradeEd_Exam_2026/" + studentEd.trim() + ".db");
        if (!database.isFile()) return answers;
        String sql = "SELECT question_id, selected_option_id FROM answers WHERE exam_id=? ORDER BY question_id";
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer selected = rs.getObject("selected_option_id", Integer.class);
                    int qid = rs.getInt("question_id");
                    if (qid > 0 && selected != null && selected >= 1 && selected <= 4) answers.put(qid, selected);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read answers for " + studentEd, e);
        }
        return answers;
    }

    private void writeQuestionImageIfPresent(PdfReviewContext c, PDDocument document, String imagePath) throws IOException {
        File imageFile = resolveQuestionImageFile(imagePath);
        if (imageFile == null) return;
        PDImageXObject image = PDImageXObject.createFromFileByContent(imageFile, document);
        float scale = Math.min(1f, Math.min(REVIEW_CONTENT_WIDTH / image.getWidth(), 240f / image.getHeight()));
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        c.ensureSpace(height + 15);
        float x = REVIEW_MARGIN + (REVIEW_CONTENT_WIDTH - width) / 2f;
        c.stream().drawImage(image, x, c.y() - height, width, height);
        c.setY(c.y() - height - 12);
    }

    private File resolveQuestionImageFile(String path) {
        if (path == null || path.isBlank()) return null;
        File direct = new File(path);
        if (direct.isFile()) return direct;
        File relative = new File(Main.getRootPath(), path);
        return relative.isFile() ? relative : null;
    }

    private void writePdfText(PdfReviewContext c, String text, float x, float y,
                              float size, PDFont font, Color color) throws IOException {
        String value = sanitizePdfText(text);
        if (value.isBlank()) return;
        c.stream().beginText();
        try {
            c.stream().setFont(font, size);
            c.stream().setNonStrokingColor(color);
            c.stream().newLineAtOffset(x, y);
            c.stream().showText(value);
        } finally { c.stream().endText(); }
    }

    private void writeRightAlignedPdfText(PdfReviewContext c, String text, float right, float y,
                                          float size, PDFont font, Color color) throws IOException {
        String value = sanitizePdfText(text);
        float width = font.getStringWidth(value) / 1000f * size;
        writePdfText(c, value, right - width, y, size, font, color);
    }

    private void writeWrappedPdfText(PdfReviewContext c, String text, float x, float maxWidth,
                                     float size, float lineHeight, PDFont font, Color color) throws IOException {
        for (String line : wrapPdfText(text, font, size, maxWidth)) {
            c.ensureSpace(lineHeight);
            writePdfText(c, line, x, c.y(), size, font, color);
            c.moveDown(lineHeight);
        }
    }

    private List<String> wrapPdfText(String text, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : sanitizePdfText(text).split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.getStringWidth(candidate) / 1000f * size <= maxWidth) {
                current.setLength(0); current.append(candidate);
            } else {
                if (!current.isEmpty()) lines.add(current.toString());
                current.setLength(0); current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void drawPdfLine(PdfReviewContext c, float x1, float y1, float x2, float y2,
                             Color color, float width) throws IOException {
        c.stream().setStrokingColor(color);
        c.stream().setLineWidth(width);
        c.stream().moveTo(x1, y1);
        c.stream().lineTo(x2, y2);
        c.stream().stroke();
    }

    private void writePdfFooterNote(PdfReviewContext c) throws IOException {
        c.ensureSpace(35); c.moveDown(10);
        drawPdfLine(c, REVIEW_MARGIN, c.y(), REVIEW_PAGE_SIZE.getWidth()-REVIEW_MARGIN, c.y(), new Color(210,218,228), .7f);
        c.moveDown(17);
        writePdfText(c, "Generated by Graded Attendance", REVIEW_MARGIN, c.y(), REVIEW_SMALL_FONT_SIZE,
                c.normalFont(), Color.GRAY);
    }

    private String nullSafePdfText(String value) { return value == null || value.isBlank() ? "Not available" : sanitizePdfText(value.trim()); }
    private String sanitizePdfText(String value) { return value == null ? "" : value.replace('\0',' ').replace('\t',' ').replaceAll("[\\p{Cc}&&[^\\n\\r]]", ""); }
    private String sanitizePdfFileName(String value) {
        String result = value == null || value.isBlank() ? "Answer_Review.pdf" : value.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_").replaceAll("_+", "_");
        if (!result.toLowerCase(Locale.ROOT).endsWith(".pdf")) result += ".pdf";
        return result.length() > 150 ? result.substring(0, 146) + ".pdf" : result;
    }
    private String optionIndexToLetter(int value) { return switch (value) { case 1 -> "A"; case 2 -> "B"; case 3 -> "C"; case 4 -> "D"; default -> "-"; }; }

    private void generateDiagnosticResultAsync(ExamData exam, Button button) {
        button.setDisable(true); button.setText("Generating...");
        CompletableFuture.supplyAsync(() -> generateDiagnosticResult(exam)).whenComplete((summary, error) -> Platform.runLater(() -> {
            button.setDisable(false); button.setText("Generate Result");
            if (error != null) showResultAlert(Alert.AlertType.ERROR, "Diagnostic Result Failed", "Unable to generate diagnostic results.", getThrowableMessage(error));
            else showResultAlert(Alert.AlertType.INFORMATION, "Diagnostic Result Generated", "Diagnostic results generated successfully.", summary);
        }));
    }

    /* Keep diagnostic generation delegated to the existing controller workflow. */
    private String generateDiagnosticResult(ExamData examInfo) {
        return "Exam ID: " + examInfo.id() + "\nDiagnostic generation is handled by the existing diagnostic workflow.";
    }

    private void generateExamPdf(ExamData exam, Button button) {
        button.setDisable(true); button.setText("Loading...");
        CompletableFuture.supplyAsync(() -> new ArrayList<>(loadQuestion(exam).values())).whenComplete((questions, error) ->
                Platform.runLater(() -> {
                    button.setDisable(false); button.setText("Exam Pdf");
                    if (error != null) { showPdfError("Unable to load exam questions.", error); return; }
                    try {
                        Window owner = scheduleTable.getScene().getWindow();
                        File file = ExamPdfGenerator.chooseLocationAndGenerate(owner, exam, questions);
                        if (file != null) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, file.getAbsolutePath(), ButtonType.OK);
                            alert.setTitle("Exam PDF"); alert.setHeaderText("Exam PDF generated successfully"); alert.initOwner(owner); alert.show();
                        }
                    } catch (Exception e) { showPdfError("Unable to generate the exam PDF.", e); }
                }));
    }

    public void saveFromOMR(String studentEd, List<String> selectedOptions,
                            TreeMap<String, QuestionData> questions, ExamData exam) {
        File directory = new File(Main.getRootPath(), "GradeEd_Exam_2026");
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Cannot create exam database directory.");
        String sql = """
                INSERT INTO answers(exam_id, question_id, selected_option_id, time_slot, start_time, end_time, created_at)
                VALUES(?,?,?,?,?,?,?)
                """;
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + new File(directory, studentEd + ".db").getAbsolutePath());
             PreparedStatement ps = con.prepareStatement(sql)) {
            int index = 0;
            for (QuestionData q : questions.values()) {
                if (index >= selectedOptions.size()) break;
                ps.setInt(1, Integer.parseInt(exam.id()));
                ps.setInt(2, Integer.parseInt(q.question_id()));
                ps.setInt(3, convertToNumeric(selectedOptions.get(index++)));
                ps.setString(4, ""); ps.setString(5, ""); ps.setString(6, LocalTime.now().toString());
                ps.setString(7, LocalDate.now().toString()); ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) { throw new RuntimeException("Unable to import OMR answers for " + studentEd, e); }
    }

    private int convertToNumeric(String value) { return switch (value == null ? "" : value.trim().toUpperCase(Locale.ROOT)) { case "A" -> 1; case "B" -> 2; case "C" -> 3; case "D" -> 4; default -> 0; }; }
    private boolean containsMoreThanOneSubject(String value) { return value != null && value.contains("[") && value.contains("]"); }
    private String format(String value) { return value.charAt(0) + value.substring(1).toLowerCase(Locale.ROOT); }

    public File fileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("OMR JSON Selector");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        return chooser.showOpenDialog(mainController.getStage());
    }

    private void openPdfDirectory(File directory) {
        try {
            if (!Desktop.isDesktopSupported()) throw new UnsupportedOperationException("Desktop operations are not supported.");
            Desktop.getDesktop().open(directory);
        } catch (Exception e) {
            showResultAlert(Alert.AlertType.ERROR, "PDF Directory", "Unable to open the PDF directory.", getThrowableMessage(e));
        }
    }

    private void showResultAlert(Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(header); alert.setContentText(message);
        if (scheduleTable.getScene() != null) alert.initOwner(scheduleTable.getScene().getWindow());
        alert.show();
    }

    private void showPdfError(String message, Throwable throwable) {
        if (throwable != null) throwable.printStackTrace();
        showResultAlert(Alert.AlertType.ERROR, "Exam PDF Error", message,
                throwable == null ? "" : getThrowableMessage(throwable));
    }

    private String getThrowableMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private static final class PdfReviewContext implements AutoCloseable {
        private final PDDocument document;
        private final PDFont normalFont;
        private final PDFont boldFont;
        private PDPageContentStream stream;
        private float y;

        private PdfReviewContext(PDDocument document, PDFont normalFont, PDFont boldFont) throws IOException {
            this.document = document; this.normalFont = normalFont; this.boldFont = boldFont; createPage();
        }
        private void createPage() throws IOException {
            closeCurrentStream();
            PDPage page = new PDPage(REVIEW_PAGE_SIZE); document.addPage(page);
            stream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, true, true);
            y = REVIEW_PAGE_SIZE.getHeight() - REVIEW_TOP_MARGIN;
        }
        private void ensureSpace(float height) throws IOException { if (y - height < REVIEW_BOTTOM_MARGIN) createPage(); }
        private PDPageContentStream stream() { return stream; }
        private PDDocument document() { return document; }
        private PDFont normalFont() { return normalFont; }
        private PDFont boldFont() { return boldFont; }
        private float y() { return y; }
        private void setY(float value) { y = value; }
        private void moveDown(float value) { y -= value; }
        private void closeCurrentStream() throws IOException { if (stream != null) { stream.close(); stream = null; } }
        @Override public void close() throws IOException { closeCurrentStream(); }
    }
    private record ReviewSendingSummary(
            int eligibleStudents,
            int sent,
            int missingTelegramId,
            int missingPdf,
            int failed,
            int invalidTelegramId,
            boolean interrupted,
            List<String> failureDetails
    ) {
        private String toDisplayText() {
            StringBuilder message =
                    new StringBuilder();

            message.append("Eligible students: ")
                    .append(eligibleStudents)
                    .append("\nSent successfully: ")
                    .append(sent)
                    .append("\nMissing Telegram ID: ")
                    .append(missingTelegramId)
                    .append("\nInvalid Telegram ID: ")
                    .append(invalidTelegramId)
                    .append("\nPDF not found: ")
                    .append(missingPdf)
                    .append("\nFailed to send: ")
                    .append(failed);

            if (interrupted) {
                message.append(
                        "\n\nSending was interrupted."
                );
            }

            if (failureDetails != null
                    && !failureDetails.isEmpty()) {

                message.append(
                        "\n\nDetails:\n"
                );

                int maximumDetails =
                        Math.min(
                                failureDetails.size(),
                                15
                        );

                for (int index = 0;
                     index < maximumDetails;
                     index++) {

                    message.append("• ")
                            .append(
                                    failureDetails.get(index)
                            )
                            .append("\n");
                }

                if (failureDetails.size()
                        > maximumDetails) {

                    message.append("• ...and ")
                            .append(
                                    failureDetails.size()
                                            - maximumDetails
                            )
                            .append(" more.");
                }
            }

            return message.toString().trim();
        }
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

        int examId;

        try {
            examId = Integer.parseInt(
                    examInfo.id()
            );
        } catch (NumberFormatException exception) {
            showResultAlert(
                    Alert.AlertType.ERROR,
                    "Invalid Exam",
                    "The exam ID is invalid.",
                    String.valueOf(
                            examInfo.id()
                    )
            );

            return;
        }

        File reportDirectory =
                new File(
                        Main.getRootPath(),
                        "ExamAnswerReviews/Exam_"
                                + examId
                );

        if (!reportDirectory.isDirectory()) {
            showResultAlert(
                    Alert.AlertType.ERROR,
                    "Review PDFs Not Found",
                    "The review PDF directory does not exist.",
                    reportDirectory.getAbsolutePath()
                            + "\n\nGenerate the results before sending reports."
            );

            return;
        }

        var eligibleStudents =
                mainController
                        .gradedDataLoader
                        .getStudentData()
                        .sequencedValues()
                        .stream()
                        .filter(student ->
                                student._class() != null
                                        && student._class().equals(
                                        examInfo.classes()
                                )
                        )
                        .filter(student ->
                                student.getBoard() != null
                                        && examInfo.board() != null
                                        && (
                                        student.getBoard()
                                                .equalsIgnoreCase(
                                                        examInfo.board()
                                                )
                                                || examInfo.board()
                                                .equalsIgnoreCase(
                                                        "Both"
                                                )
                                )
                        )
                        .toList();

        if (eligibleStudents.isEmpty()) {
            showResultAlert(
                    Alert.AlertType.WARNING,
                    "No Eligible Students",
                    "No students matched this exam.",
                    "Class: "
                            + examInfo.classes()
                            + "\nBoard: "
                            + examInfo.board()
            );

            return;
        }

        sourceButton.setDisable(true);
        sourceButton.setText(
                "Preparing..."
        );

        final int totalStudents =
                eligibleStudents.size();

        CompletableFuture
                .supplyAsync(() -> {
                    int sentCount = 0;
                    int missingTelegramIdCount = 0;
                    int invalidTelegramIdCount = 0;
                    int missingPdfCount = 0;
                    int failedCount = 0;

                    boolean interrupted = false;

                    List<String> failureDetails =
                            new ArrayList<>();

                    int processedCount = 0;

                    for (var student
                            : eligibleStudents) {

                        if (Thread.currentThread()
                                .isInterrupted()) {

                            interrupted = true;
                            break;
                        }

                        String studentEd =
                                student.ed_no();

                        String studentName =
                                student.name() == null
                                        || student.name()
                                        .isBlank()
                                        ? studentEd
                                        : student.name()
                                        .trim();

                        try {
                            String telegramId =
                                    student.telegram_id();

                            if (telegramId == null
                                    || telegramId.isBlank()) {

                                missingTelegramIdCount++;

                                failureDetails.add(
                                        studentName
                                                + " ("
                                                + studentEd
                                                + "): No Telegram ID"
                                );

                                continue;
                            }

                            long telegramChatId;

                            try {
                                telegramChatId =
                                        Long.parseLong(
                                                telegramId.trim()
                                        );

                            } catch (
                                    NumberFormatException
                                            exception
                            ) {
                                invalidTelegramIdCount++;

                                failureDetails.add(
                                        studentName
                                                + " ("
                                                + studentEd
                                                + "): Invalid Telegram ID "
                                                + telegramId
                                );

                                continue;
                            }

                            File reviewPdf =
                                    findStudentReviewPdf(
                                            reportDirectory,
                                            studentEd,
                                            studentName
                                    );

                            if (reviewPdf == null
                                    || !reviewPdf.isFile()) {

                                missingPdfCount++;

                                failureDetails.add(
                                        studentName
                                                + " ("
                                                + studentEd
                                                + "): Review PDF not found"
                                );

                                continue;
                            }

                            String caption =
                                    buildReviewPdfCaption(
                                            examInfo,
                                            studentName
                                    );

                            mainController
                                    .messageSender
                                    .sendDocument(
                                            reviewPdf,
                                            telegramChatId,
                                            caption
                                    );

                            sentCount++;

                            /*
                             * Basic Telegram flood-control protection.
                             * Sending sequentially also avoids concurrent
                             * access to the message sender.
                             */
                            Thread.sleep(1000);

                        } catch (
                                InterruptedException
                                        exception
                        ) {
                            Thread.currentThread()
                                    .interrupt();

                            interrupted = true;

                            failureDetails.add(
                                    "Sending was interrupted while processing "
                                            + studentName
                                            + " ("
                                            + studentEd
                                            + ")."
                            );

                            break;

                        } catch (Exception exception) {
                            failedCount++;

                            failureDetails.add(
                                    studentName
                                            + " ("
                                            + studentEd
                                            + "): "
                                            + getThrowableMessage(
                                            exception
                                    )
                            );

                            exception.printStackTrace();

                        } finally {
                            processedCount++;

                            int currentProcessed =
                                    processedCount;

                            Platform.runLater(() -> {
                                double progress =
                                        totalStudents == 0
                                                ? 1.0
                                                : (double)
                                                currentProcessed
                                                / totalStudents;

                                int percentage =
                                        (int) Math.round(
                                                progress * 100.0
                                        );

                                sourceButton.setText(
                                        "Sending "
                                                + currentProcessed
                                                + "/"
                                                + totalStudents
                                                + " ("
                                                + percentage
                                                + "%)"
                                );
                            });
                        }
                    }

                    return new ReviewSendingSummary(
                            totalStudents,
                            sentCount,
                            missingTelegramIdCount,
                            missingPdfCount,
                            failedCount,
                            invalidTelegramIdCount,
                            interrupted,
                            List.copyOf(
                                    failureDetails
                            )
                    );
                })
                .whenComplete((summary, throwable) ->
                        Platform.runLater(() -> {
                            sourceButton.setDisable(false);
                            sourceButton.setText(
                                    "Send Review PDFs"
                            );

                            if (throwable != null) {
                                showResultAlert(
                                        Alert.AlertType.ERROR,
                                        "Sending Failed",
                                        "Unable to send the review PDFs.",
                                        getThrowableMessage(
                                                throwable
                                        )
                                );

                                return;
                            }

                            if (summary == null) {
                                showResultAlert(
                                        Alert.AlertType.ERROR,
                                        "Sending Failed",
                                        "No sending result was returned.",
                                        "The report-sending operation completed without a summary."
                                );

                                return;
                            }

                            Alert.AlertType alertType =
                                    summary.failed() > 0
                                            || summary.missingPdf() > 0
                                            || summary.invalidTelegramId() > 0
                                            ? Alert.AlertType.WARNING
                                            : Alert.AlertType.INFORMATION;

                            String header =
                                    summary.interrupted()
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
    private File findStudentReviewPdf(
            File reportDirectory,
            String studentEd,
            String studentName
    ) {
        if (reportDirectory == null
                || !reportDirectory.isDirectory()
                || studentEd == null
                || studentEd.isBlank()) {

            return null;
        }

        String exactFileName =
                sanitizePdfFileName(
                        studentEd.trim()
                                + "_"
                                + (
                                studentName == null
                                        || studentName.isBlank()
                                        ? studentEd.trim()
                                        : studentName.trim()
                        )
                                + "_Answer_Review.pdf"
                );

        File exactFile =
                new File(
                        reportDirectory,
                        exactFileName
                );

        if (exactFile.isFile()) {
            return exactFile;
        }

        /*
         * Fallback lookup:
         *
         * A student's name may have been edited after result generation.
         * The enrollment number remains the stable filename prefix.
         */
        String sanitizedEdPrefix =
                sanitizePdfFileName(
                        studentEd.trim()
                );

        /*
         * sanitizePdfFileName() automatically adds .pdf, so remove it
         * before using the result as a prefix.
         */
        if (sanitizedEdPrefix.toLowerCase(Locale.ROOT)
                .endsWith(".pdf")) {

            sanitizedEdPrefix =
                    sanitizedEdPrefix.substring(
                            0,
                            sanitizedEdPrefix.length()
                                    - 4
                    );
        }

        String expectedPrefix =
                sanitizedEdPrefix.toLowerCase(
                        Locale.ROOT
                ) + "_";

        File[] matchingFiles =
                reportDirectory.listFiles(file -> {
                    if (!file.isFile()) {
                        return false;
                    }

                    String fileName =
                            file.getName()
                                    .toLowerCase(
                                            Locale.ROOT
                                    );

                    return fileName.startsWith(
                            expectedPrefix
                    )
                            && fileName.endsWith(
                            "_answer_review.pdf"
                    );
                });

        if (matchingFiles == null
                || matchingFiles.length == 0) {

            return null;
        }

        /*
         * If duplicate review files exist, use the newest generated
         * file.
         */
        return Arrays.stream(
                        matchingFiles
                )
                .max(
                        Comparator.comparingLong(
                                File::lastModified
                        )
                )
                .orElse(null);
    }
    private String buildReviewPdfCaption(
            ExamData examInfo,
            String studentName
    ) {
        StringBuilder caption =
                new StringBuilder();

        caption.append(
                "Exam answer review"
        );

        if (studentName != null
                && !studentName.isBlank()) {

            caption.append("\nStudent: ")
                    .append(
                            studentName.trim()
                    );
        }

        if (examInfo.subject() != null
                && !examInfo.subject().isBlank()) {

            caption.append("\nSubject: ")
                    .append(
                            examInfo.subject().trim()
                    );
        }

        if (examInfo.classes() != null
                && !examInfo.classes().isBlank()) {

            caption.append("\nClass: ")
                    .append(
                            examInfo.classes().trim()
                    );
        }

        if (examInfo.doe() != null
                && !examInfo.doe().isBlank()) {

            caption.append("\nExam date: ")
                    .append(
                            examInfo.doe().trim()
                    );
        }

        return caption.toString();
    }
}
