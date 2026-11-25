package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.RingProgressIndicator;
import com.dlsc.gemsfx.SVGImageView;
import com.dlsc.gemsfx.SearchField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.controller.home.MainController;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DiagnosticExamReport implements Initializable {

    private static final int SNAPSHOT_SCALE = 3;

    private final MainController mainController;

    @FXML
    private Text avatar;

    @FXML
    private Label class_of;

    @FXML
    private Label grade;

    @FXML
    private Label marks_obtain;

    @FXML
    private Label name_class;

    @FXML
    private Label remarks;

    @FXML
    private Label roll_no;

    @FXML
    private Label total_marks;

    @FXML
    private Label overAllRemark;

    @FXML
    private Label totalRemark;

    @FXML
    private Label rank;

    @FXML
    private SVGImageView icons;

    @FXML
    private RingProgressIndicator allSubjectPercentage;

    @FXML
    private VBox report;

    @FXML
    private VBox mathResult;

    @FXML
    private VBox basicScienceResult;

    @FXML
    private HBox performanceLayer;

    @FXML
    private SearchField<String> search;

    private final ObservableList<String> diagnosticStudents =
            FXCollections.observableArrayList();

    /*
     * The diagnostic report currently supports exactly two subject boxes:
     *
     * 1. Math
     * 2. Basic Science
     */
    private List<VBox> allSubjectBoxes = List.of();

    public DiagnosticExamReport(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(
            URL location,
            ResourceBundle resources
    ) {
        icons.setSvgUrl(
                GradedResourceLoader.load("icons/my-logo.svg")
        );

        allSubjectBoxes = List.of(
                mathResult,
                basicScienceResult
        );

        hideReport();
        loadDiagnosticStudentNames();

        search.setSuggestionProvider(request -> {

            String searchText = request.getUserText();

            if (searchText == null || searchText.isBlank()) {
                return new ArrayList<>(diagnosticStudents);
            }

            String normalizedSearch = searchText
                    .trim()
                    .toLowerCase(Locale.ROOT);

            return diagnosticStudents.stream()
                    .filter(name -> name
                            .toLowerCase(Locale.ROOT)
                            .contains(normalizedSearch))
                    .collect(Collectors.toList());
        });

        search.setOnCommit(this::generateDiagnosticReport);
    }

    /**
     * Loads distinct diagnostic candidate names from DiagnosticScoreCard.
     *
     * Diagnostic candidates do not need to exist in StudentData.
     */
    private void loadDiagnosticStudentNames() {

        diagnosticStudents.clear();

        String sql = """
                SELECT DISTINCT TRIM(student_name) AS student_name
                FROM DiagnosticScoreCard
                WHERE student_name IS NOT NULL
                  AND TRIM(student_name) <> ''
                ORDER BY student_name COLLATE NOCASE
                """;

        var connection =
                mainController
                        .gradedDataLoader
                        .databaseLoader
                        .getConnection();

        /*
         * Removes case-insensitive duplicate names.
         *
         * Example:
         *
         * Aman Kumar
         * aman kumar
         *
         * will be displayed as only one candidate.
         */
        Map<String, String> uniqueNames =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try (var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                String studentName = normalizeStudentName(
                        resultSet.getString("student_name")
                );

                if (!studentName.isBlank()) {
                    uniqueNames.putIfAbsent(
                            studentName,
                            studentName
                    );
                }
            }

            diagnosticStudents.setAll(
                    uniqueNames.values()
            );

        } catch (SQLException exception) {

            exception.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Diagnostic Report",
                    "Unable to load diagnostic students",
                    getExceptionMessage(exception)
            );
        }
    }

    /**
     * Called by the SearchField when a candidate is selected.
     */
    private int generateDiagnosticReport(String selectedName) {
        return generateDiagnosticReport(
                selectedName,
                true
        );
    }

    /**
     * Generates and displays one diagnostic candidate's report.
     *
     * @param selectedName candidate name
     * @param showMessages true to display information and error alerts
     *
     * @return 1 if generated successfully
     *         0 if no attempted result exists
     *        -1 if an error occurred
     */
    private int generateDiagnosticReport(
            String selectedName,
            boolean showMessages
    ) {
        if (selectedName == null || selectedName.isBlank()) {
            hideReport();
            return -1;
        }

        String studentName =
                normalizeStudentName(selectedName);

        /*
         * Subject normalization:
         *
         * Math, Maths and Mathematics become Math.
         *
         * Basic Science, Science, General Science, Physics, Chemistry
         * and Biology become Basic Science.
         *
         * The Physics, Chemistry and Biology aliases allow older database
         * records to remain compatible.
         */
        String sql = """
                SELECT
                    CASE
                        WHEN LOWER(TRIM(subject)) IN
                             (
                                 'math',
                                 'maths',
                                 'mathematics'
                             )
                            THEN 'Math'

                        WHEN LOWER(TRIM(subject)) IN
                             (
                                 'basic science',
                                 'basic sciences',
                                 'basicscience',
                                 'basic_science',
                                 'science',
                                 'general science',
                                 'general sciences',
                                 'physics',
                                 'chemistry',
                                 'biology'
                             )
                            THEN 'Basic Science'

                        ELSE TRIM(subject)
                    END AS normalized_subject,

                    COUNT(DISTINCT exam_id) AS exam_count,

                    COALESCE(
                        SUM(marks_obtain),
                        0
                    ) AS total_obtained,

                    COALESCE(
                        SUM(total_marks),
                        0
                    ) AS total_possible,

                    COALESCE(
                        SUM(total_attempted),
                        0
                    ) AS attempted_answers,

                    COALESCE(
                        SUM(correct_answers),
                        0
                    ) AS correct_answers,

                    COALESCE(
                        SUM(wrong_answers),
                        0
                    ) AS wrong_answers

                FROM DiagnosticScoreCard

                WHERE student_name = ? COLLATE NOCASE
                  AND LOWER(
                          TRIM(
                              COALESCE(remark, '')
                          )
                      ) <> 'not attempted'

                GROUP BY
                    CASE
                        WHEN LOWER(TRIM(subject)) IN
                             (
                                 'math',
                                 'maths',
                                 'mathematics'
                             )
                            THEN 'Math'

                        WHEN LOWER(TRIM(subject)) IN
                             (
                                 'basic science',
                                 'basic sciences',
                                 'basicscience',
                                 'basic_science',
                                 'science',
                                 'general science',
                                 'general sciences',
                                 'physics',
                                 'chemistry',
                                 'biology'
                             )
                            THEN 'Basic Science'

                        ELSE TRIM(subject)
                    END

                ORDER BY normalized_subject COLLATE NOCASE
                """;

        Map<String, DiagnosticSubjectSummary> subjectWiseResult =
                new LinkedHashMap<>();

        var connection =
                mainController
                        .gradedDataLoader
                        .databaseLoader
                        .getConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentName);

            try (var resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    String subject = normalizeSubject(
                            resultSet.getString(
                                    "normalized_subject"
                            )
                    );

                    if (subject.isBlank()) {
                        continue;
                    }

                    /*
                     * The report has only Math and Basic Science boxes.
                     *
                     * Ignore unsupported database subjects instead of adding
                     * them to the overall total without displaying them.
                     */
                    if (!isSupportedSubject(subject)) {
                        continue;
                    }

                    int examCount =
                            resultSet.getInt("exam_count");

                    int totalObtained =
                            resultSet.getInt("total_obtained");

                    int totalPossible =
                            resultSet.getInt("total_possible");

                    int attemptedAnswers =
                            resultSet.getInt("attempted_answers");

                    int correctAnswers =
                            resultSet.getInt("correct_answers");

                    int wrongAnswers =
                            resultSet.getInt("wrong_answers");

                    double percentage =
                            calculatePercentage(
                                    totalObtained,
                                    totalPossible
                            );

                    DiagnosticSubjectSummary summary =
                            new DiagnosticSubjectSummary(
                                    subject,
                                    examCount,
                                    totalObtained,
                                    totalPossible,
                                    attemptedAnswers,
                                    correctAnswers,
                                    wrongAnswers,
                                    percentage
                            );

                    subjectWiseResult.put(
                            subject,
                            summary
                    );
                }
            }

        } catch (SQLException exception) {

            exception.printStackTrace();
            hideReport();

            if (showMessages) {
                showAlert(
                        Alert.AlertType.ERROR,
                        "Diagnostic Report",
                        "Unable to generate diagnostic report",
                        getExceptionMessage(exception)
                );
            }

            return -1;
        }

        if (subjectWiseResult.isEmpty()) {

            hideReport();

            if (showMessages) {
                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Diagnostic Report",
                        "No attempted diagnostic result found",
                        "No attempted Math or Basic Science "
                                + "diagnostic result was found for "
                                + studentName
                                + "."
                );
            }

            return 0;
        }

        resetSubjectBoxes();

        int overallObtained = 0;
        int overallPossible = 0;
        int overallAttempted = 0;
        int overallCorrect = 0;
        int overallWrong = 0;
        int totalExams = 0;

        Set<VBox> availableSubjectBoxes =
                new HashSet<>();

        for (DiagnosticSubjectSummary summary
                : subjectWiseResult.values()) {

            VBox subjectBox =
                    getSubjectBox(summary.subject());

            if (subjectBox == null) {
                continue;
            }

            plotData(
                    subjectBox,
                    summary.percentage() / 100.0,
                    summary.totalObtained()
                            + "/"
                            + summary.totalPossible(),
                    getGrade(
                            summary.percentage()
                    ).grade()
            );

            subjectBox.setVisible(true);
            subjectBox.setManaged(true);

            availableSubjectBoxes.add(subjectBox);

            overallObtained +=
                    summary.totalObtained();

            overallPossible +=
                    summary.totalPossible();

            overallAttempted +=
                    summary.attemptedAnswers();

            overallCorrect +=
                    summary.correctAnswers();

            overallWrong +=
                    summary.wrongAnswers();

            totalExams +=
                    summary.examCount();
        }

        hideUnusedSubjectBoxes(
                availableSubjectBoxes
        );

        double overallPercentage =
                calculatePercentage(
                        overallObtained,
                        overallPossible
                );

        GradeResult gradeResult =
                getGrade(overallPercentage);

        avatar.setText(
                getNameAbbreviation(studentName)
        );

        name_class.setText(studentName);

        /*
         * Diagnostic candidates are not required to be registered students.
         */
        class_of.setText("Diagnostic Test");
        roll_no.setText("Unregistered Candidate");

        allSubjectPercentage.setProgress(
                clampProgress(
                        overallPercentage / 100.0
                )
        );

        marks_obtain.setText(
                String.valueOf(overallObtained)
        );

        total_marks.setText(
                " / " + overallPossible
        );

        grade.setText(
                gradeResult.grade()
        );

        overAllRemark.setText(
                gradeResult.remark()
        );

        totalRemark.setText(
                gradeResult.remark()
        );

        remarks.setText(
                buildTeacherRemark(
                        gradeResult,
                        totalExams,
                        overallAttempted,
                        overallCorrect,
                        overallWrong
                )
        );

        /*
         * Diagnostic candidates are not ranked against enrolled students.
         */
        rank.setText("N/A");

        report.setManaged(true);
        report.setVisible(true);

        /*
         * Ensure the report has been styled and laid out before a PDF
         * snapshot is requested.
         */
        report.applyCss();
        report.layout();

        return 1;
    }

    /**
     * Generates PDF reports for every diagnostic candidate currently stored
     * in DiagnosticScoreCard.
     */
    @FXML
    void generateReport(ActionEvent event) {

        File reportFolder = new File(
                Main.getRootPath(),
                "My Drive/DiagnosticReport"
        );

        if (!reportFolder.exists()
                && !reportFolder.mkdirs()) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Diagnostic Report",
                    "Unable to create report directory",
                    reportFolder.getAbsolutePath()
            );

            return;
        }

        if (diagnosticStudents.isEmpty()) {

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Diagnostic Reports",
                    "No diagnostic candidates found",
                    "Generate the diagnostic results before "
                            + "creating the reports."
            );

            return;
        }

        Button button = (Button) event.getSource();
        button.setDisable(true);

        List<String> students =
                new ArrayList<>(diagnosticStudents);

        Task<BatchReportResult> reportTask =
                new Task<>() {

                    @Override
                    protected BatchReportResult call() {

                        int generated = 0;
                        int skipped = 0;
                        int failed = 0;

                        List<String> errors =
                                new ArrayList<>();

                        int total = students.size();

                        for (int index = 0;
                             index < students.size();
                             index++) {

                            if (isCancelled()) {
                                break;
                            }

                            String studentName =
                                    students.get(index);

                            CompletableFuture<Integer> reportFuture =
                                    new CompletableFuture<>();

                            /*
                             * JavaFX controls, CSS, layout and snapshot
                             * operations must happen on the JavaFX
                             * Application Thread.
                             */
                            Platform.runLater(() -> {

                                try {
                                    int result =
                                            generateDiagnosticReport(
                                                    studentName,
                                                    false
                                            );

                                    if (result == 1) {

                                        File outputFile =
                                                new File(
                                                        reportFolder,
                                                        createSafeFileName(
                                                                studentName
                                                        )
                                                                + "_diagnostic_report.pdf"
                                                );

                                        savePaneAsPdf(
                                                report,
                                                outputFile.getAbsolutePath()
                                        );
                                    }

                                    reportFuture.complete(result);

                                } catch (Exception exception) {

                                    reportFuture.completeExceptionally(
                                            exception
                                    );
                                }
                            });

                            try {
                                int result =
                                        reportFuture.join();

                                if (result == 1) {
                                    generated++;
                                } else {
                                    skipped++;
                                }

                            } catch (Exception exception) {

                                failed++;

                                Throwable actualException =
                                        unwrapException(exception);

                                errors.add(
                                        studentName
                                                + ": "
                                                + getExceptionMessage(
                                                actualException
                                        )
                                );
                            }

                            updateProgress(
                                    index + 1,
                                    total
                            );

                            updateMessage(
                                    (index + 1)
                                            + " / "
                                            + total
                                            + " : "
                                            + studentName
                            );
                        }

                        return new BatchReportResult(
                                generated,
                                skipped,
                                failed,
                                List.copyOf(errors)
                        );
                    }
                };

        reportTask.setOnSucceeded(workerStateEvent -> {

            button.setDisable(false);

            BatchReportResult result =
                    reportTask.getValue();

            StringBuilder message =
                    new StringBuilder()
                            .append("Generated: ")
                            .append(result.generated())
                            .append("\nSkipped: ")
                            .append(result.skipped())
                            .append("\nFailed: ")
                            .append(result.failed())
                            .append("\n\nReports were saved to:\n")
                            .append(
                                    reportFolder.getAbsolutePath()
                            );

            if (!result.errors().isEmpty()) {

                message.append("\n\nErrors:\n");

                int maximumShown =
                        Math.min(
                                10,
                                result.errors().size()
                        );

                for (int index = 0;
                     index < maximumShown;
                     index++) {

                    message.append("• ")
                            .append(
                                    result.errors().get(index)
                            )
                            .append('\n');
                }

                if (result.errors().size()
                        > maximumShown) {

                    message.append("• ...and ")
                            .append(
                                    result.errors().size()
                                            - maximumShown
                            )
                            .append(" more.");
                }
            }

            Alert.AlertType alertType =
                    result.failed() == 0
                            ? Alert.AlertType.INFORMATION
                            : Alert.AlertType.WARNING;

            showAlert(
                    alertType,
                    "Diagnostic Reports",
                    "Report generation completed",
                    message.toString()
            );
        });

        reportTask.setOnFailed(workerStateEvent -> {

            button.setDisable(false);

            Throwable exception =
                    reportTask.getException();

            if (exception != null) {
                exception.printStackTrace();
            }

            showAlert(
                    Alert.AlertType.ERROR,
                    "Diagnostic Reports",
                    "Report generation failed",
                    getExceptionMessage(exception)
            );
        });

        reportTask.setOnCancelled(workerStateEvent ->
                button.setDisable(false)
        );

        Thread thread = new Thread(
                reportTask,
                "diagnostic-report-generator"
        );

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Restores and clears both subject boxes before displaying another
     * candidate.
     */
    private void resetSubjectBoxes() {

        for (VBox subjectBox : allSubjectBoxes) {

            clearSubjectBox(subjectBox);

            subjectBox.setVisible(true);
            subjectBox.setManaged(true);
        }
    }

    /**
     * Clears old marks, grades and progress from one subject box.
     */
    private void clearSubjectBox(VBox subjectBox) {

        if (subjectBox == null) {
            return;
        }

        RingProgressIndicator progressIndicator =
                findProgressIndicator(subjectBox);

        Label marksLabel =
                findLabel(subjectBox, "#marks");

        Label gradeLabel =
                findGradeLabel(subjectBox);

        if (progressIndicator != null) {
            progressIndicator.setProgress(0);
        }

        if (marksLabel != null) {
            marksLabel.setText("0/0");
        }

        if (gradeLabel != null) {
            gradeLabel.setText("-");
        }
    }

    /**
     * Hides Math or Basic Science when the candidate has no attempted result
     * for that subject.
     */
    private void hideUnusedSubjectBoxes(
            Set<VBox> availableSubjectBoxes
    ) {
        for (VBox subjectBox : allSubjectBoxes) {

            boolean available =
                    availableSubjectBoxes.contains(subjectBox);

            subjectBox.setVisible(available);
            subjectBox.setManaged(available);
        }
    }

    /**
     * Returns the FXML box matching the normalized subject.
     */
    private VBox getSubjectBox(String subject) {

        return switch (normalizeSubject(subject)) {

            case "Math" ->
                    mathResult;

            case "Basic Science" ->
                    basicScienceResult;

            default ->
                    null;
        };
    }

    /**
     * Returns true only for the two subjects supported by this report.
     */
    private boolean isSupportedSubject(String subject) {

        String normalized =
                normalizeSubject(subject);

        return "Math".equals(normalized)
                || "Basic Science".equals(normalized);
    }

    /**
     * Displays a subject's percentage, marks and grade.
     */
    private void plotData(
            VBox box,
            double progress,
            String marks,
            String gradeText
    ) {
        if (box == null) {
            return;
        }

        RingProgressIndicator progressIndicator =
                findProgressIndicator(box);

        Label marksLabel =
                findLabel(box, "#marks");

        Label gradeLabel =
                findGradeLabel(box);

        if (progressIndicator != null) {
            progressIndicator.setProgress(
                    clampProgress(progress)
            );
        }

        if (marksLabel != null) {
            marksLabel.setText(marks);
        }

        if (gradeLabel != null) {
            gradeLabel.setText(gradeText);
        }
    }

    /**
     * Finds the nested progress indicator.
     *
     * The existing FXML is expected to use fx:id="percentage".
     */
    private RingProgressIndicator findProgressIndicator(
            VBox subjectBox
    ) {
        var node = subjectBox.lookup("#percentage");

        if (node instanceof RingProgressIndicator indicator) {
            return indicator;
        }

        return null;
    }

    /**
     * Finds a nested label safely without a direct unsafe cast.
     */
    private Label findLabel(
            VBox subjectBox,
            String selector
    ) {
        var node = subjectBox.lookup(selector);

        if (node instanceof Label label) {
            return label;
        }

        return null;
    }

    /**
     * Supports both the old misspelled FXML ID "garde" and the corrected
     * FXML ID "subjectGrade".
     */
    private Label findGradeLabel(VBox subjectBox) {

        Label label =
                findLabel(subjectBox, "#subjectGrade");

        if (label != null) {
            return label;
        }

        label = findLabel(
                subjectBox,
                "#garde"
        );

        if (label != null) {
            return label;
        }

        return findLabel(
                subjectBox,
                "#grade"
        );
    }

    private double calculatePercentage(
            int obtained,
            int possible
    ) {
        if (possible <= 0) {
            return 0;
        }

        double percentage =
                obtained * 100.0 / possible;

        if (!Double.isFinite(percentage)) {
            return 0;
        }

        return Math.max(
                0,
                Math.min(100, percentage)
        );
    }

    private double clampProgress(double progress) {

        if (!Double.isFinite(progress)) {
            return 0;
        }

        return Math.max(
                0,
                Math.min(1, progress)
        );
    }

    private String buildTeacherRemark(
            GradeResult gradeResult,
            int examCount,
            int attemptedAnswers,
            int correctAnswers,
            int wrongAnswers
    ) {
        String examText = examCount == 1
                ? "1 diagnostic exam"
                : examCount + " diagnostic exams";

        return gradeResult.teacherRemark()
                + " Summary: "
                + examText
                + ", "
                + attemptedAnswers
                + " attempted, "
                + correctAnswers
                + " correct and "
                + wrongAnswers
                + " incorrect.";
    }

    private String normalizeStudentName(String name) {

        if (name == null) {
            return "";
        }

        return name
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Normalizes every accepted Math and Basic Science alias.
     */
    private String normalizeSubject(String subject) {

        if (subject == null) {
            return "";
        }

        String normalized =
                subject.trim();

        return switch (
                normalized.toLowerCase(Locale.ROOT)
                ) {
            case "math",
                 "maths",
                 "mathematics" ->
                    "Math";

            case "basic science",
                 "basic sciences",
                 "basicscience",
                 "basic_science",
                 "science",
                 "general science",
                 "general sciences",
                 "physics",
                 "chemistry",
                 "biology" ->
                    "Basic Science";

            default ->
                    normalized;
        };
    }

    private String getNameAbbreviation(String name) {

        if (name == null || name.isBlank()) {
            return "NA";
        }

        String[] parts =
                name.trim().split("\\s+");

        if (parts.length == 1) {

            return parts[0]
                    .substring(
                            0,
                            Math.min(
                                    2,
                                    parts[0].length()
                            )
                    )
                    .toUpperCase(Locale.ROOT);
        }

        return (
                String.valueOf(
                        parts[0].charAt(0)
                )
                        + parts[
                        parts.length - 1
                        ].charAt(0)
        ).toUpperCase(Locale.ROOT);
    }

    /**
     * Creates a filename that is valid on Windows.
     */
    private String createSafeFileName(
            String studentName
    ) {
        String safeName =
                normalizeStudentName(studentName)
                        .replaceAll(
                                "[\\\\/:*?\"<>|]",
                                "_"
                        )
                        .replaceAll("[. ]+$", "")
                        .trim();

        return safeName.isBlank()
                ? "diagnostic_student"
                : safeName;
    }

    public GradeResult getGrade(double percentage) {

        if (!Double.isFinite(percentage)) {
            percentage = 0;
        }

        percentage = Math.max(
                0,
                Math.min(100, percentage)
        );

        if (percentage >= 90) {
            return new GradeResult(
                    "A+",
                    "Outstanding",
                    "Exceptional performance. Keep up the excellent work."
            );
        }

        if (percentage >= 80) {
            return new GradeResult(
                    "A",
                    "Excellent",
                    "Excellent work. Continue striving for even greater achievement."
            );
        }

        if (percentage >= 70) {
            return new GradeResult(
                    "B+",
                    "Very Good",
                    "Very good performance. With consistent effort, you can achieve excellence."
            );
        }

        if (percentage >= 60) {
            return new GradeResult(
                    "B",
                    "Good",
                    "Good progress. Continue practising to strengthen your understanding."
            );
        }

        if (percentage >= 50) {
            return new GradeResult(
                    "C",
                    "Satisfactory",
                    "Satisfactory performance. More regular practice will help you improve."
            );
        }

        if (percentage >= 40) {
            return new GradeResult(
                    "D",
                    "Needs Improvement",
                    "You have shown some understanding, but focused effort is needed."
            );
        }

        if (percentage >= 33) {
            return new GradeResult(
                    "E",
                    "Passed",
                    "You have passed, but you should revise the fundamentals and practise regularly."
            );
        }

        return new GradeResult(
                "F",
                "Failed",
                "You need significant improvement. Please seek guidance and work consistently."
        );
    }

    /**
     * Saves the visible report as a single A4 PDF.
     *
     * This method must run on the JavaFX Application Thread because CSS,
     * layout and snapshot operations access JavaFX controls.
     */
    private void savePaneAsPdf(
            VBox pane,
            String outputFile
    ) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "savePaneAsPdf must run on the "
                            + "JavaFX Application Thread."
            );
        }

        File destination =
                new File(outputFile);

        File parentDirectory =
                destination.getParentFile();

        if (parentDirectory != null
                && !parentDirectory.exists()
                && !parentDirectory.mkdirs()) {

            throw new IllegalStateException(
                    "Could not create report directory: "
                            + parentDirectory.getAbsolutePath()
            );
        }

        pane.applyCss();
        pane.layout();

        SnapshotParameters parameters =
                new SnapshotParameters();

        parameters.setTransform(
                new Scale(
                        SNAPSHOT_SCALE,
                        SNAPSHOT_SCALE
                )
        );

        parameters.setFill(Color.WHITE);

        WritableImage snapshot =
                pane.snapshot(parameters, null);

        if (snapshot.getWidth() <= 0
                || snapshot.getHeight() <= 0) {

            throw new IllegalStateException(
                    "The diagnostic report has "
                            + "no visible content."
            );
        }

        try (PDDocument document = new PDDocument()) {

            PDPage page =
                    new PDPage(PDRectangle.A4);

            document.addPage(page);

            PDImageXObject pdfImage =
                    LosslessFactory.createFromImage(
                            document,
                            SwingFXUtils.fromFXImage(
                                    snapshot,
                                    null
                            )
                    );

            float margin = 24f;

            float pageWidth =
                    page.getMediaBox().getWidth();

            float pageHeight =
                    page.getMediaBox().getHeight();

            float availableWidth =
                    pageWidth - (margin * 2);

            float availableHeight =
                    pageHeight - (margin * 2);

            float imageWidth =
                    pdfImage.getWidth();

            float imageHeight =
                    pdfImage.getHeight();

            float imageScale =
                    Math.min(
                            availableWidth / imageWidth,
                            availableHeight / imageHeight
                    );

            float drawWidth =
                    imageWidth * imageScale;

            float drawHeight =
                    imageHeight * imageScale;

            float x =
                    (pageWidth - drawWidth) / 2f;

            float y =
                    (pageHeight - drawHeight) / 2f;

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(
                                 document,
                                 page
                         )) {

                contentStream.drawImage(
                        pdfImage,
                        x,
                        y,
                        drawWidth,
                        drawHeight
                );
            }

            document.save(destination);

            System.out.println(
                    "Diagnostic PDF saved: "
                            + destination.getAbsolutePath()
            );

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save diagnostic report PDF: "
                            + outputFile,
                    exception
            );
        }
    }

    private void hideReport() {

        if (report != null) {
            report.setVisible(false);
            report.setManaged(false);
        }
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String header,
            String content
    ) {
        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(header);

        alert.setContentText(
                content == null
                        ? ""
                        : content
        );

        if (report != null
                && report.getScene() != null
                && report.getScene().getWindow() != null) {

            alert.initOwner(
                    report.getScene().getWindow()
            );
        }

        alert.show();
    }

    private Throwable unwrapException(
            Throwable exception
    ) {
        Throwable current = exception;

        while (current.getCause() != null
                && current.getCause() != current) {

            current = current.getCause();
        }

        return current;
    }

    private String getExceptionMessage(
            Throwable exception
    ) {
        if (exception == null) {
            return "Unknown error";
        }

        String message =
                exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }

    public record DiagnosticSubjectSummary(
            String subject,
            int examCount,
            int totalObtained,
            int totalPossible,
            int attemptedAnswers,
            int correctAnswers,
            int wrongAnswers,
            double percentage
    ) {
    }

    public record GradeResult(
            String grade,
            String remark,
            String teacherRemark
    ) {
    }

    private record BatchReportResult(
            int generated,
            int skipped,
            int failed,
            List<String> errors
    ) {
    }
}