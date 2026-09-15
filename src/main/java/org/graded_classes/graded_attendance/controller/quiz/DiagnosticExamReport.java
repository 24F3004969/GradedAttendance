package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.RingProgressIndicator;
import com.dlsc.gemsfx.SVGImageView;
import com.dlsc.gemsfx.SearchField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.controller.home.MainController;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DiagnosticExamReport implements Initializable {

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
    private VBox physicsResult;

    @FXML
    private VBox biologyResult;

    @FXML
    private VBox chemistryResult;

    @FXML
    private VBox englishResult;

    @FXML
    private HBox performanceLayer;

    @FXML
    private SearchField<String> search;

    private final ObservableList<String> diagnosticStudents =
            FXCollections.observableArrayList();

    /*
     * These are the original subject boxes from the FXML.
     * They are needed because removing a subject box from performanceLayer
     * would otherwise remove it permanently when another student is selected.
     */
    private List<VBox> allSubjectBoxes;

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
                physicsResult,
                chemistryResult,
                biologyResult,
                englishResult
        );

        report.setVisible(false);
        report.setManaged(false);

        loadDiagnosticStudentNames();

        search.setSuggestionProvider(request -> {

            String searchText = request.getUserText();

            if (searchText == null || searchText.isBlank()) {
                return new ArrayList<>(diagnosticStudents);
            }

            String normalizedSearch =
                    searchText.trim().toLowerCase(Locale.ROOT);

            return diagnosticStudents
                    .stream()
                    .filter(name -> name
                            .toLowerCase(Locale.ROOT)
                            .contains(normalizedSearch))
                    .collect(Collectors.toList());
        });

        search.setOnCommit(this::generateDiagnosticReport);
    }

    private void loadDiagnosticStudentNames() {

        diagnosticStudents.clear();

        String sql = """
                SELECT DISTINCT student_name
                FROM DiagnosticScoreCard
                WHERE student_name IS NOT NULL
                  AND TRIM(student_name) <> ''
                ORDER BY student_name COLLATE NOCASE
                """;

        var connection = mainController
                .gradedDataLoader
                .databaseLoader
                .getConnection();

        try (
                var statement = connection.prepareStatement(sql);
                var resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {

                String studentName =
                        resultSet.getString("student_name");

                if (studentName != null
                        && !studentName.isBlank()) {

                    diagnosticStudents.add(
                            normalizeStudentName(studentName)
                    );
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Diagnostic Report",
                    "Unable to load diagnostic students",
                    exception.getMessage()
            );
        }
    }

    private String normalizeStudentName(String name) {

        if (name == null) {
            return "";
        }

        return name
                .trim()
                .replaceAll("\\s+", " ");
    }

    private int generateDiagnosticReport(String selectedName) {

        if (selectedName == null || selectedName.isBlank()) {
            hideReport();
            return -1;
        }

        String studentName =
                normalizeStudentName(selectedName);

        String sql = """
                SELECT
                    subject,
                    COUNT(DISTINCT exam_id) AS exam_count,
                    COALESCE(SUM(marks_obtain), 0) AS total_obtained,
                    COALESCE(SUM(total_marks), 0) AS total_possible,
                    COALESCE(SUM(correct_answers), 0) AS correct_answers,
                    COALESCE(SUM(wrong_answers), 0) AS wrong_answers
                FROM DiagnosticScoreCard
                WHERE student_name = ? COLLATE NOCASE
                GROUP BY subject
                ORDER BY subject COLLATE NOCASE
                """;

        Map<String, DiagnosticSubjectSummary> subjectWiseResult =
                new LinkedHashMap<>();

        var connection = mainController
                .gradedDataLoader
                .databaseLoader
                .getConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentName);

            try (var resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    String subject =
                            normalizeSubject(
                                    resultSet.getString("subject")
                            );

                    int examCount =
                            resultSet.getInt("exam_count");

                    int totalObtained =
                            resultSet.getInt("total_obtained");

                    int totalPossible =
                            resultSet.getInt("total_possible");

                    int correctAnswers =
                            resultSet.getInt("correct_answers");

                    int wrongAnswers =
                            resultSet.getInt("wrong_answers");

                    double percentage = totalPossible == 0
                            ? 0
                            : totalObtained * 100.0 / totalPossible;

                    DiagnosticSubjectSummary summary =
                            new DiagnosticSubjectSummary(
                                    subject,
                                    examCount,
                                    totalObtained,
                                    totalPossible,
                                    correctAnswers,
                                    wrongAnswers,
                                    percentage
                            );

                    subjectWiseResult.put(subject, summary);
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
            hideReport();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Diagnostic Report",
                    "Unable to generate diagnostic report",
                    exception.getMessage()
            );

            return -1;
        }

        if (subjectWiseResult.isEmpty()) {
            hideReport();

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Diagnostic Report",
                    "No diagnostic result found",
                    "No result was found for " + studentName
            );

            return -1;
        }

        resetSubjectBoxes();

        int overallObtained = 0;
        int overallPossible = 0;

        for (DiagnosticSubjectSummary summary
                : subjectWiseResult.values()) {

            VBox subjectBox =
                    getSubjectBox(summary.subject());

            if (subjectBox != null) {

                plotData(
                        subjectBox,
                        summary.percentage() / 100.0,
                        summary.totalObtained()
                                + "/"
                                + summary.totalPossible(),
                        getGrade(summary.percentage()).grade()
                );

                subjectBox.setVisible(true);
                subjectBox.setManaged(true);
            }

            overallObtained += summary.totalObtained();
            overallPossible += summary.totalPossible();
        }

        hideUnusedSubjectBoxes(subjectWiseResult.keySet());

        double overallPercentage = overallPossible == 0
                ? 0
                : overallObtained * 100.0 / overallPossible;

        GradeResult gradeResult =
                getGrade(overallPercentage);

        avatar.setText(
                getNameAbbreviation(studentName)
        );

        name_class.setText(studentName);

        /*
         * A diagnostic candidate does not have a registered class or ED number.
         */
        class_of.setText("Diagnostic Test");
        roll_no.setText("Unregistered Candidate");

        allSubjectPercentage.setProgress(
                overallPercentage / 100.0
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
                gradeResult.teacherRemark()
        );

        /*
         * Diagnostic candidates are normally not ranked against registered
         * students. You can implement diagnostic ranking separately if needed.
         */
        rank.setText("N/A");

        report.setManaged(true);
        report.setVisible(true);

        return 1;
    }

    public record DiagnosticSubjectSummary(
            String subject,
            int examCount,
            int totalObtained,
            int totalPossible,
            int correctAnswers,
            int wrongAnswers,
            double percentage
    ) {
    }

    private VBox getSubjectBox(String subject) {

        return switch (normalizeSubject(subject)) {

            case "Math" -> mathResult;

            case "Physics" -> physicsResult;

            case "Chemistry" -> chemistryResult;

            case "Biology" -> biologyResult;

            case "English" -> englishResult;

            default -> null;
        };
    }

    private void resetSubjectBoxes() {

        if (!performanceLayer.getChildren()
                .containsAll(allSubjectBoxes)) {

            performanceLayer
                    .getChildren()
                    .setAll(allSubjectBoxes);
        }

        for (VBox subjectBox : allSubjectBoxes) {
            subjectBox.setVisible(true);
            subjectBox.setManaged(true);
        }
    }

    private void hideUnusedSubjectBoxes(
            Set<String> availableSubjects
    ) {
        Set<String> normalizedSubjects =
                availableSubjects
                        .stream()
                        .map(this::normalizeSubject)
                        .collect(Collectors.toSet());

        for (VBox subjectBox : allSubjectBoxes) {

            boolean available =
                    normalizedSubjects.contains(
                            subjectBox.getId()
                    );

            subjectBox.setVisible(available);
            subjectBox.setManaged(available);
        }
    }

    private void plotData(
            VBox box,
            double percentage,
            String marks,
            String gradeText
    ) {
        RingProgressIndicator progressIndicator =
                (RingProgressIndicator) box.lookup("#percentage");

        Label marksLabel =
                (Label) box.lookup("#marks");

        Label gradeLabel =
                (Label) box.lookup("#garde");

        if (progressIndicator != null) {
            progressIndicator.setProgress(percentage);
        }

        if (marksLabel != null) {
            marksLabel.setText(marks);
        }

        if (gradeLabel != null) {
            gradeLabel.setText(gradeText);
        }
    }

    public record GradeResult(
            String grade,
            String remark,
            String teacherRemark
    ) {
    }

    private String getNameAbbreviation(String name) {

        if (name == null || name.isBlank()) {
            return "NA";
        }

        String[] parts = name
                .trim()
                .split("\\s+");

        if (parts.length == 1) {
            return parts[0]
                    .substring(
                            0,
                            Math.min(2, parts[0].length())
                    )
                    .toUpperCase(Locale.ROOT);
        }

        return (
                String.valueOf(parts[0].charAt(0))
                        + parts[parts.length - 1].charAt(0)
        ).toUpperCase(Locale.ROOT);
    }

    private void hideReport() {
        report.setVisible(false);
        report.setManaged(false);
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
        alert.setContentText(content);

        if (report.getScene() != null) {
            alert.initOwner(
                    report.getScene().getWindow()
            );
        }

        alert.show();
    }

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

        Button button = (Button) event.getSource();
        button.setDisable(true);

        List<String> students =
                new ArrayList<>(diagnosticStudents);

        Task<Void> reportTask = new Task<>() {

            @Override
            protected Void call() throws Exception {

                int total = students.size();

                for (int index = 0;
                     index < students.size();
                     index++) {

                    String studentName =
                            students.get(index);

                    /*
                     * A snapshot and all JavaFX control changes must happen
                     * on the JavaFX Application Thread.
                     */
                    CompletableFuture<Void> reportFuture =
                            new CompletableFuture<>();

                    Platform.runLater(() -> {

                        try {
                            int result =
                                    generateDiagnosticReport(
                                            studentName
                                    );

                            if (result == 1) {

                                File outputFile = new File(
                                        reportFolder,
                                        createSafeFileName(studentName)
                                                + "_diagnostic_report.pdf"
                                );

                                savePaneAsPdf(
                                        report,
                                        outputFile.getAbsolutePath()
                                );
                            }

                            reportFuture.complete(null);

                        } catch (Exception exception) {
                            reportFuture.completeExceptionally(
                                    exception
                            );
                        }
                    });

                    reportFuture.join();

                    updateProgress(index + 1, total);

                    updateMessage(
                            (index + 1)
                                    + " / "
                                    + total
                                    + " : "
                                    + studentName
                    );
                }

                return null;
            }
        };

        reportTask.setOnSucceeded(workerStateEvent -> {

            button.setDisable(false);

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Diagnostic Reports",
                    "Report generation completed",
                    "Reports were saved to:\n"
                            + reportFolder.getAbsolutePath()
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
                    exception == null
                            ? "Unknown error"
                            : exception.getMessage()
            );
        });

        Thread thread = new Thread(
                reportTask,
                "diagnostic-report-generator"
        );

        thread.setDaemon(true);
        thread.start();
    }

    private String createSafeFileName(String studentName) {

        String safeName = normalizeStudentName(studentName)
                .replaceAll("[\\\\/:*?\"<>|]", "_");

        return safeName.isBlank()
                ? "diagnostic_student"
                : safeName;
    }

    private String normalizeSubject(String subject) {

        if (subject == null) {
            return "";
        }

        return switch (subject.trim().toLowerCase()) {

            case "math":
            case "maths":
            case "mathematics":
                yield "Math";

            case "physics":
                yield "Physics";

            case "chemistry":
                yield "Chemistry";

            case "biology":
                yield "Biology";

            case "english":
                yield "English";

            default:
                yield subject.trim();
        };
    }

    public GradeResult getGrade(double percentage) {

        if (Double.isNaN(percentage)
                || Double.isInfinite(percentage)) {
            percentage = 0;
        }

        // Prevent values outside the valid range.
        percentage = Math.max(0, Math.min(100, percentage));

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
    private void savePaneAsPdf(
            VBox pane,
            String outputFile
    ) {
        try {
            // Ensure the destination directory exists.
            File destination = new File(outputFile);
            File parentDirectory = destination.getParentFile();

            if (parentDirectory != null && !parentDirectory.exists()) {
                parentDirectory.mkdirs();
            }

            // Force CSS and layout before taking the snapshot.
            pane.applyCss();
            pane.layout();

            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setTransform(new Scale(3, 3));
            parameters.setFill(Color.WHITE);

            WritableImage snapshot = pane.snapshot(
                    parameters,
                    null
            );

            if (snapshot.getWidth() <= 0
                    || snapshot.getHeight() <= 0) {
                throw new IllegalStateException(
                        "The diagnostic report has no visible content."
                );
            }

            try (PDDocument document = new PDDocument()) {

                PDRectangle pageSize = new PDRectangle(
                        (float) snapshot.getWidth(),
                        (float) snapshot.getHeight()
                );

                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                PDImageXObject pdfImage =
                        LosslessFactory.createFromImage(
                                document,
                                SwingFXUtils.fromFXImage(
                                        snapshot,
                                        null
                                )
                        );

                float pageWidth =
                        page.getMediaBox().getWidth();

                float pageHeight =
                        page.getMediaBox().getHeight();

                float imageWidth =
                        pdfImage.getWidth();

                float imageHeight =
                        pdfImage.getHeight();

                float scale = Math.min(
                        pageWidth / imageWidth,
                        pageHeight / imageHeight
                );

                float drawWidth =
                        imageWidth * scale;

                float drawHeight =
                        imageHeight * scale;

                float x =
                        (pageWidth - drawWidth) / 2f;

                float y =
                        (pageHeight - drawHeight) / 2f;

                try (
                        PDPageContentStream contentStream =
                                new PDPageContentStream(
                                        document,
                                        page
                                )
                ) {
                    contentStream.drawImage(
                            pdfImage,
                            x,
                            y,
                            drawWidth,
                            drawHeight
                    );
                }

                document.save(destination);
            }

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
}

