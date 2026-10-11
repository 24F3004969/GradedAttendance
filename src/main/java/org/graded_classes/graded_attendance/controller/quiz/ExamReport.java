package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.RingProgressIndicator;
import com.dlsc.gemsfx.DialogPane;
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
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.Student;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class ExamReport implements Initializable {

    private static final String REPORT_DIRECTORY =
            "C:\\Users\\hilal\\StudentReport\\";

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

    private final ObservableList<String> studentData =
            FXCollections.observableArrayList();

    private final Map<String, VBox> subjectBoxes =
            new LinkedHashMap<>();

    private LinkedHashMap<String, Student> data;

    private final VBox dialogContent =
            new VBox();

    private final ProgressBar generation =
            new ProgressBar();

    private final ScrollPane listOfGenerated =
            new ScrollPane();

    private final VBox sentItems =
            new VBox();

    public ExamReport(MainController mainController) {
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

        data = mainController
                .gradedDataLoader
                .getStudentData();

        studentData.setAll(
                createStudentSearchList(data.values())
        );

        search.setSuggestionProvider(request -> {
            String searchText = request
                    .getUserText()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            return studentData.stream()
                    .filter(student ->
                            student.toLowerCase(Locale.ROOT)
                                    .contains(searchText)
                    )
                    .collect(Collectors.toList());
        });

        search.setOnCommit(this::generateReport);

        dialogContent.getChildren().setAll(
                generation,
                listOfGenerated
        );

        /*
         * Canonical subject name -> subject report VBox.
         *
         * The subject VBoxes are never removed from performanceLayer.
         * They are shown or hidden using managed and visible properties.
         */
        subjectBoxes.put("Math", mathResult);
        subjectBoxes.put("Physics", physicsResult);
        subjectBoxes.put("Chemistry", chemistryResult);
        subjectBoxes.put("Biology", biologyResult);
        subjectBoxes.put("English", englishResult);

        resetSubjectBoxes();
    }

    /**
     * Generates a single student's report in the displayed report pane.
     *
     * @return 1 when generated, otherwise -1
     */
    private int generateReport(String selectedStudent) {
        if (selectedStudent == null
                || selectedStudent.isBlank()) {

            return -1;
        }

        String[] studentParts =
                selectedStudent.trim().split("\\s+", 2);

        if (studentParts.length < 2) {
            showError(
                    "Invalid student selection",
                    "Expected student data in the format: "
                            + "enrollment-number student-name"
            );

            return -1;
        }

        String roll = studentParts[0].trim();
        String name = studentParts[1].trim();

        Student student = data.get(roll);

        if (student == null) {
            showError(
                    "Student not found",
                    "No student record was found for enrollment number "
                            + roll + "."
            );

            return -1;
        }

        /*
         * Restore and clear all subject boxes before every report.
         *
         * This prevents a subject hidden for one student from remaining
         * hidden for the next student during batch generation.
         */
        resetSubjectBoxes();

        LinkedHashMap<String, StudentRank> resultList =
                getClassRank(student._class());

        String sql = """
                SELECT
                    TRIM(subject) AS subject,
                    COUNT(*) AS exam_count,
                    COALESCE(SUM(marks_obtain), 0) AS total_obtained,
                    COALESCE(SUM(total_marks), 0) AS total_possible,
                    COALESCE(AVG(marks_obtain), 0) AS average_marks
                FROM ScoreCard
                WHERE ed_no = ?
                GROUP BY TRIM(subject)
                ORDER BY TRIM(subject)
                """;

        Map<String, SubjectScoreSummary> subjectWiseResult =
                new LinkedHashMap<>();

        var connection = mainController
                .gradedDataLoader
                .databaseLoader
                .getConnection();

        try (var preparedStatement =
                     connection.prepareStatement(sql)) {

            preparedStatement.setString(1, roll);

            try (var resultSet =
                         preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    String rawSubject =
                            resultSet.getString("subject");

                    String normalizedSubject =
                            normalizeSubject(rawSubject);

                    int examCount =
                            resultSet.getInt("exam_count");

                    int totalObtained =
                            resultSet.getInt("total_obtained");

                    int totalPossible =
                            resultSet.getInt("total_possible");

                    double averageMarks =
                            resultSet.getDouble("average_marks");

                    double percentage =
                            calculatePercentage(
                                    totalObtained,
                                    totalPossible
                            );

                    SubjectScoreSummary summary =
                            new SubjectScoreSummary(
                                    normalizedSubject,
                                    examCount,
                                    totalObtained,
                                    totalPossible,
                                    averageMarks,
                                    percentage
                            );

                    /*
                     * Merge database aliases.
                     *
                     * For example, "Math", "Maths", and "Mathematics"
                     * all become the canonical subject "Math".
                     */
                    subjectWiseResult.merge(
                            normalizedSubject,
                            summary,
                            this::mergeSubjectSummaries
                    );
                }
            }

            if (subjectWiseResult.isEmpty()) {
                report.setVisible(false);
                report.setManaged(false);

                System.out.println(
                        "No ScoreCard records found for roll number: "
                                + roll
                );

                return -1;
            }

            avatar.setText(
                    getNameAbbreviation(name)
            );

            name_class.setText(name);
            roll_no.setText("Roll No. " + roll);
            class_of.setText("Class " + student._class());

            Set<String> displayedSubjects =
                    new LinkedHashSet<>();

            double overallObtained = 0.0;
            double overallPossible = 0.0;

            for (SubjectScoreSummary summary
                    : subjectWiseResult.values()) {

                overallObtained +=
                        summary.totalObtained();

                overallPossible +=
                        summary.totalPossible();

                VBox subjectBox =
                        subjectBoxes.get(summary.subject());

                if (subjectBox == null) {
                    System.err.println(
                            "No report box exists for subject '"
                                    + summary.subject()
                                    + "' for enrollment number "
                                    + roll
                    );

                    /*
                     * Unknown subjects still contribute to the overall
                     * percentage, but cannot be displayed because there
                     * is no corresponding subject VBox in the FXML.
                     */
                    continue;
                }

                displayedSubjects.add(summary.subject());

                showSubjectBox(subjectBox);

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
            }

            /*
             * Hide only unavailable subjects.
             *
             * The nodes remain attached to performanceLayer, so they can
             * be made visible again for the next student.
             */
            updateSubjectBoxVisibility(displayedSubjects);

            double overallPercentage =
                    calculatePercentage(
                            overallObtained,
                            overallPossible
                    );

            allSubjectPercentage.setProgress(
                    clampProgress(
                            overallPercentage / 100.0
                    )
            );

            marks_obtain.setText(
                    formatNumber(overallObtained)
            );

            total_marks.setText(
                    " / " + formatNumber(overallPossible)
            );

            GradeResult overallGrade =
                    getGrade(overallPercentage);

            grade.setText(
                    overallGrade.grade()
            );

            StudentRank studentRank =
                    resultList.get(roll);

            rank.setText(
                    studentRank == null
                            ? "None"
                            : String.valueOf(
                            studentRank.rank()
                    )
            );

            overAllRemark.setText(
                    overallGrade.remark()
            );

            totalRemark.setText(
                    overallGrade.remark()
            );

            remarks.setText(
                    overallGrade.teacherRemark()
            );

            report.setManaged(true);
            report.setVisible(true);

            /*
             * Ensure CSS and layout are completed before a PDF snapshot
             * is taken during batch generation.
             */
            report.applyCss();
            report.layout();

            return 1;

        } catch (SQLException exception) {
            report.setVisible(false);
            report.setManaged(false);

            exception.printStackTrace();

            showError(
                    "Unable to generate report",
                    "The score data for enrollment number "
                            + roll
                            + " could not be loaded.\n\n"
                            + exception.getMessage()
            );

            return -1;
        }
    }

    /**
     * Restores every subject box and clears data from the previous report.
     */
    private void resetSubjectBoxes() {
        for (VBox subjectBox : subjectBoxes.values()) {
            showSubjectBox(subjectBox);
            clearSubjectBox(subjectBox);
        }
    }

    /**
     * Shows subjects that have results and hides subjects that do not.
     *
     * setManaged(false) ensures that a hidden subject does not continue
     * occupying space in the HBox.
     */
    private void updateSubjectBoxVisibility(
            Set<String> availableSubjects
    ) {
        for (Map.Entry<String, VBox> entry
                : subjectBoxes.entrySet()) {

            boolean available =
                    availableSubjects.contains(
                            entry.getKey()
                    );

            entry.getValue().setVisible(available);
            entry.getValue().setManaged(available);
        }
    }

    private void showSubjectBox(VBox subjectBox) {
        if (subjectBox == null) {
            return;
        }

        subjectBox.setManaged(true);
        subjectBox.setVisible(true);
    }

    private void clearSubjectBox(VBox subjectBox) {
        if (subjectBox == null) {
            return;
        }

        RingProgressIndicator percentageIndicator =
                lookupRingProgressIndicator(
                        subjectBox,
                        "#percentage"
                );

        Label marksLabel =
                lookupLabel(
                        subjectBox,
                        "#marks"
                );

        /*
         * Your existing FXML appears to use the ID "garde".
         *
         * This method also tries "grade" as a fallback in case the FXML
         * spelling is later corrected.
         */
        Label gradeLabel =
                lookupLabel(
                        subjectBox,
                        "#garde"
                );

        if (gradeLabel == null) {
            gradeLabel = lookupLabel(
                    subjectBox,
                    "#grade"
            );
        }

        if (percentageIndicator != null) {
            percentageIndicator.setProgress(0);
        }

        if (marksLabel != null) {
            marksLabel.setText("0/0");
        }

        if (gradeLabel != null) {
            gradeLabel.setText("-");
        }
    }

    private void plotData(
            VBox subjectBox,
            double progress,
            String marksText,
            String gradeText
    ) {
        if (subjectBox == null) {
            return;
        }

        subjectBox.applyCss();
        subjectBox.layout();

        RingProgressIndicator percentageIndicator =
                lookupRingProgressIndicator(
                        subjectBox,
                        "#percentage"
                );

        Label marksLabel =
                lookupLabel(
                        subjectBox,
                        "#marks"
                );

        Label gradeLabel =
                lookupLabel(
                        subjectBox,
                        "#garde"
                );

        if (gradeLabel == null) {
            gradeLabel = lookupLabel(
                    subjectBox,
                    "#grade"
            );
        }

        if (percentageIndicator != null) {
            percentageIndicator.setProgress(
                    clampProgress(progress)
            );
        } else {
            System.err.println(
                    "RingProgressIndicator with id 'percentage' "
                            + "was not found in subject box: "
                            + subjectBox.getId()
            );
        }

        if (marksLabel != null) {
            marksLabel.setText(marksText);
        } else {
            System.err.println(
                    "Label with id 'marks' was not found "
                            + "in subject box: "
                            + subjectBox.getId()
            );
        }

        if (gradeLabel != null) {
            gradeLabel.setText(gradeText);
        } else {
            System.err.println(
                    "Label with id 'garde' or 'grade' was not found "
                            + "in subject box: "
                            + subjectBox.getId()
            );
        }
    }

    private RingProgressIndicator lookupRingProgressIndicator(
            VBox subjectBox,
            String selector
    ) {
        var node = subjectBox.lookup(selector);

        if (node instanceof RingProgressIndicator indicator) {
            return indicator;
        }

        return null;
    }

    private Label lookupLabel(
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
     * Converts different database subject names into canonical names.
     */
    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "Unknown";
        }

        String normalized = subject
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        return switch (normalized) {
            case "math",
                 "maths",
                 "mathematics",
                 "mathematic" -> "Math";

            case "physics",
                 "physic",
                 "phy" -> "Physics";

            case "chemistry",
                 "chemical science",
                 "chem" -> "Chemistry";

            case "biology",
                 "biological science",
                 "bio" -> "Biology";

            case "english",
                 "english language",
                 "eng" -> "English";

            default -> toDisplayCase(subject.trim());
        };
    }

    private String toDisplayCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String[] words =
                value.trim().split("\\s+");

        StringBuilder result =
                new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(word.charAt(0))
            );

            if (word.length() > 1) {
                result.append(
                        word.substring(1)
                                .toLowerCase(Locale.ROOT)
                );
            }
        }

        return result.toString();
    }

    /**
     * Merges summaries when multiple subject aliases exist.
     */
    private SubjectScoreSummary mergeSubjectSummaries(
            SubjectScoreSummary first,
            SubjectScoreSummary second
    ) {
        int examCount =
                first.examCount()
                        + second.examCount();

        int totalObtained =
                first.totalObtained()
                        + second.totalObtained();

        int totalPossible =
                first.totalPossible()
                        + second.totalPossible();

        double averageMarks =
                examCount == 0
                        ? 0.0
                        : totalObtained * 1.0 / examCount;

        double percentage =
                calculatePercentage(
                        totalObtained,
                        totalPossible
                );

        return new SubjectScoreSummary(
                first.subject(),
                examCount,
                totalObtained,
                totalPossible,
                averageMarks,
                percentage
        );
    }

    private double calculatePercentage(
            double obtained,
            double possible
    ) {
        if (possible <= 0) {
            return 0.0;
        }

        double percentage =
                obtained * 100.0 / possible;

        if (Double.isNaN(percentage)
                || Double.isInfinite(percentage)) {

            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(100.0, percentage)
        );
    }

    private double clampProgress(double progress) {
        if (Double.isNaN(progress)
                || Double.isInfinite(progress)) {

            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(1.0, progress)
        );
    }

    private String formatNumber(double number) {
        if (number == Math.rint(number)) {
            return String.valueOf((long) number);
        }

        return String.format(
                Locale.ROOT,
                "%.2f",
                number
        );
    }

    private String getNameAbbreviation(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        String[] parts =
                name.trim().split("\\s+");

        if (parts.length > 1) {
            return (String.valueOf(parts[0].charAt(0))
                    + parts[parts.length - 1].charAt(0))
                    .toUpperCase(Locale.ROOT);
        }

        String firstName = parts[0];

        if (firstName.length() >= 2) {
            return firstName
                    .substring(0, 2)
                    .toUpperCase(Locale.ROOT);
        }

        return firstName.toUpperCase(Locale.ROOT);
    }

    private List<String> createStudentSearchList(
            Collection<Student> students
    ) {
        List<String> result =
                new ArrayList<>();

        if (students == null) {
            return result;
        }

        for (Student student : students) {
            if (student == null) {
                continue;
            }

            result.add(
                    student.ed_no()
                            + " "
                            + student.name()
            );
        }

        return result;
    }

    @FXML
    void sendAllReport(ActionEvent event) {
        sentItems.getChildren().clear();

        listOfGenerated.setContent(sentItems);

        generation.progressProperty().unbind();
        generation.setProgress(0);

        Button button =
                (Button) event.getSource();

        button.setDisable(true);

        dialogWhileGeneration(
                "Report sending progress",
                "Sending the reports"
        );

        File reportFolder =
                new File(REPORT_DIRECTORY);

        CompletableFuture.runAsync(() -> {
            int total = data.size();
            int current = 0;

            for (Map.Entry<String, Student> entry
                    : data.entrySet()) {

                String roll = entry.getKey();
                Student student = entry.getValue();

                try {
                    String telegramId =
                            student.telegram_id();

                    if (telegramId != null
                            && !telegramId.isBlank()) {

                        File pdf = new File(
                                reportFolder,
                                roll + "_report.pdf"
                        );

                        if (pdf.exists()
                                && pdf.isFile()) {

                            mainController.messageSender.sendDocument(
                                    pdf,
                                    Long.parseLong(
                                            telegramId.trim()
                                    ),
                                    "Progress report"
                            );

                            addSendingStatus(
                                    "✅ Sent: "
                                            + student.name()
                                            + " ("
                                            + roll
                                            + ")"
                            );
                        } else {
                            addSendingStatus(
                                    "❌ PDF not found: "
                                            + student.name()
                                            + " ("
                                            + roll
                                            + ")"
                            );
                        }
                    } else {
                        addSendingStatus(
                                "⚠ No Telegram ID: "
                                        + student.name()
                                        + " ("
                                        + roll
                                        + ")"
                        );
                    }

                    /*
                     * Basic Telegram rate-limit protection.
                     */
                    Thread.sleep(1000);

                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();

                    addSendingStatus(
                            "⚠ Sending interrupted."
                    );

                    break;

                } catch (Exception exception) {
                    addSendingStatus(
                            "❌ Failed: "
                                    + student.name()
                                    + " ("
                                    + roll
                                    + ") - "
                                    + exception.getMessage()
                    );

                    exception.printStackTrace();
                }

                current++;

                double progress =
                        total == 0
                                ? 1.0
                                : (double) current / total;

                Platform.runLater(() ->
                        generation.setProgress(progress)
                );
            }
        }).whenComplete((result, exception) ->
                Platform.runLater(() -> {
                    button.setDisable(false);
                    generation.setProgress(1);

                    if (exception != null) {
                        exception.printStackTrace();

                        showError(
                                "Report sending failed",
                                exception.getMessage()
                        );

                        return;
                    }

                    Alert alert = new Alert(
                            Alert.AlertType.INFORMATION,
                            "All reports have been processed."
                    );

                    alert.setHeaderText(
                            "Report sending completed"
                    );

                    alert.show();
                })
        );
    }

    private void addSendingStatus(String message) {
        Platform.runLater(() ->
                sentItems.getChildren().add(
                        new Label(message)
                )
        );
    }

    @FXML
    void generateReport(ActionEvent event) {
        File outputFolder =
                new File(REPORT_DIRECTORY);

        if (!outputFolder.exists()
                && !outputFolder.mkdirs()) {

            showError(
                    "Unable to create report folder",
                    "The application could not create:\n"
                            + outputFolder.getAbsolutePath()
            );

            return;
        }

        Button button =
                (Button) event.getSource();

        button.setDisable(true);

        dialogWhileGeneration(
                "Report generation progress",
                "Generating reports"
        );

        Task<Void> reportTask =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        int total = data.size();
                        int current = 0;

                        for (Map.Entry<String, Student> entry
                                : data.entrySet()) {

                            if (isCancelled()) {
                                break;
                            }

                            String roll =
                                    entry.getKey();

                            Student studentData =
                                    entry.getValue();

                            String studentSearchValue =
                                    roll
                                            + " "
                                            + studentData.name();

                            CountDownLatch latch =
                                    new CountDownLatch(1);

                            final Throwable[] error =
                                    new Throwable[1];

                            Platform.runLater(() -> {
                                try {
                                    int result =
                                            generateReport(
                                                    studentSearchValue
                                            );

                                    if (result == 1) {
                                        File outputFile =
                                                new File(
                                                        outputFolder,
                                                        roll
                                                                + "_report.pdf"
                                                );

                                        savePaneAsPdf(
                                                report,
                                                outputFile.getAbsolutePath()
                                        );
                                    }
                                } catch (Throwable throwable) {
                                    error[0] = throwable;
                                } finally {
                                    latch.countDown();
                                }
                            });

                            latch.await();

                            if (error[0] != null) {
                                throw new RuntimeException(
                                        "Failed to generate report for "
                                                + roll,
                                        error[0]
                                );
                            }

                            current++;

                            updateProgress(
                                    current,
                                    total
                            );

                            updateMessage(
                                    current
                                            + " / "
                                            + total
                                            + " reports generated"
                            );
                        }

                        return null;
                    }
                };

        generation.progressProperty().unbind();
        generation.progressProperty().bind(
                reportTask.progressProperty()
        );

        reportTask.messageProperty().addListener(
                (observable, oldValue, newValue) ->
                        listOfGenerated.setContent(
                                new Label(newValue)
                        )
        );

        reportTask.setOnSucceeded(workerStateEvent -> {
            button.setDisable(false);

            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION,
                    "All available reports were generated successfully."
            );

            alert.setHeaderText(
                    "Report generation completed"
            );

            alert.show();

            System.out.println(
                    "Report generation completed."
            );
        });

        reportTask.setOnFailed(workerStateEvent -> {
            button.setDisable(false);

            Throwable exception =
                    reportTask.getException();

            if (exception != null) {
                exception.printStackTrace();
            }

            showError(
                    "Report generation failed",
                    exception == null
                            ? "An unknown error occurred."
                            : exception.getMessage()
            );
        });

        reportTask.setOnCancelled(workerStateEvent ->
                button.setDisable(false)
        );

        Thread thread =
                new Thread(
                        reportTask,
                        "student-report-generator"
                );

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Must be called on the JavaFX Application Thread.
     */
    private void savePaneAsPdf(
            VBox pane,
            String outputFile
    ) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "PDF snapshot must be created on "
                            + "the JavaFX Application Thread."
            );
        }

        if (pane == null) {
            throw new IllegalArgumentException(
                    "Report pane cannot be null."
            );
        }

        pane.applyCss();
        pane.layout();

        SnapshotParameters parameters =
                new SnapshotParameters();

        parameters.setTransform(
                new Scale(3, 3)
        );

        parameters.setFill(Color.WHITE);

        WritableImage image =
                pane.snapshot(
                        parameters,
                        null
                );

        PDRectangle pageSize =
                new PDRectangle(
                        (float) image.getWidth(),
                        (float) image.getHeight()
                );

        try (PDDocument document =
                     new PDDocument()) {

            PDPage page =
                    new PDPage(pageSize);

            document.addPage(page);

            PDImageXObject pdfImage =
                    LosslessFactory.createFromImage(
                            document,
                            SwingFXUtils.fromFXImage(
                                    image,
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

            float scale =
                    Math.min(
                            pageWidth / imageWidth,
                            pageHeight / imageHeight
                    );

            float drawWidth =
                    imageWidth * scale;

            float drawHeight =
                    imageHeight * scale;

            float x =
                    (pageWidth - drawWidth) / 2.0f;

            float y =
                    (pageHeight - drawHeight) / 2.0f;

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

            document.save(outputFile);

            System.out.println(
                    "High-quality PDF saved: "
                            + outputFile
            );

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Failed to save PDF: "
                            + outputFile,
                    exception
            );
        }
    }

    public record SubjectScoreSummary(
            String subject,
            int examCount,
            int totalObtained,
            int totalPossible,
            double averageMarks,
            double percentage
    ) {
    }

    public record GradeResult(
            String grade,
            String remark,
            String teacherRemark
    ) {
    }

    public GradeResult getGrade(double percentage) {
        if (Double.isNaN(percentage)
                || Double.isInfinite(percentage)
                || percentage < 0
                || percentage > 100) {

            throw new IllegalArgumentException(
                    "Percentage must be between 0 and 100. Found: "
                            + percentage
            );
        }

        if (percentage >= 90) {
            return new GradeResult(
                    "A+",
                    "Outstanding",
                    "Exceptional performance. "
                            + "Keep up the excellent work."
            );
        }

        if (percentage >= 80) {
            return new GradeResult(
                    "A",
                    "Excellent",
                    "Excellent work. Continue striving "
                            + "for even greater achievement."
            );
        }

        if (percentage >= 70) {
            return new GradeResult(
                    "B+",
                    "Very Good",
                    "Very good performance. With consistent "
                            + "effort, you can achieve excellence."
            );
        }

        if (percentage >= 60) {
            return new GradeResult(
                    "B",
                    "Good",
                    "Good progress. Continue practising "
                            + "to strengthen your understanding."
            );
        }

        if (percentage >= 50) {
            return new GradeResult(
                    "C",
                    "Satisfactory",
                    "Satisfactory performance. More regular "
                            + "practice will help you improve."
            );
        }

        if (percentage >= 40) {
            return new GradeResult(
                    "D",
                    "Needs Improvement",
                    "You have shown some understanding, "
                            + "but focused effort is needed."
            );
        }

        if (percentage >= 33) {
            return new GradeResult(
                    "E",
                    "Passed",
                    "You have passed, but you should revise "
                            + "the fundamentals and practise regularly."
            );
        }

        return new GradeResult(
                "F",
                "Failed",
                "You need significant improvement. "
                        + "Please seek guidance and work consistently."
        );
    }

    public record StudentRank(
            int rank,
            String edNo,
            String name,
            String studentClass,
            int examsCount,
            int totalObtained,
            int totalPossible,
            double percentage,
            String grade,
            String teacherRemark
    ) {
    }

    public LinkedHashMap<String, StudentRank> getClassRank(
            String className
    ) {
        String sql = """
                WITH StudentTotals AS
                (
                    SELECT
                        sd.ed_no,
                        sd.name,
                        sd.class AS student_class,
                        COUNT(sc.exam_id) AS exams_count,
                        COALESCE(SUM(sc.marks_obtain), 0) AS total_obtained,
                        COALESCE(SUM(sc.total_marks), 0) AS total_possible,

                        CASE
                            WHEN COALESCE(SUM(sc.total_marks), 0) = 0
                                THEN 0
                            ELSE
                                COALESCE(SUM(sc.marks_obtain), 0) * 100.0
                                / SUM(sc.total_marks)
                        END AS percentage

                    FROM StudentData sd

                    LEFT JOIN ScoreCard sc
                        ON sc.ed_no = sd.ed_no

                    WHERE sd.class = ?

                    GROUP BY
                        sd.ed_no,
                        sd.name,
                        sd.class
                ),

                EligibleStudents AS
                (
                    SELECT *
                    FROM StudentTotals
                    WHERE percentage >= 40
                )

                SELECT
                    DENSE_RANK() OVER (
                        ORDER BY percentage DESC
                    ) AS student_rank,

                    ed_no,
                    name,
                    student_class,
                    exams_count,
                    total_obtained,
                    total_possible,
                    percentage

                FROM EligibleStudents

                ORDER BY
                    student_rank,
                    name
                """;

        LinkedHashMap<String, StudentRank> rankedStudents =
                new LinkedHashMap<>();

        var connection = mainController
                .gradedDataLoader
                .databaseLoader
                .getConnection();

        try (var preparedStatement =
                     connection.prepareStatement(sql)) {

            preparedStatement.setString(
                    1,
                    className
            );

            try (var resultSet =
                         preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    double percentage =
                            resultSet.getDouble(
                                    "percentage"
                            );

                    GradeResult gradeResult =
                            getGrade(
                                    Math.max(
                                            0.0,
                                            Math.min(
                                                    100.0,
                                                    percentage
                                            )
                                    )
                            );

                    StudentRank studentRank =
                            new StudentRank(
                                    resultSet.getInt(
                                            "student_rank"
                                    ),
                                    resultSet.getString(
                                            "ed_no"
                                    ),
                                    resultSet.getString(
                                            "name"
                                    ),
                                    resultSet.getString(
                                            "student_class"
                                    ),
                                    resultSet.getInt(
                                            "exams_count"
                                    ),
                                    resultSet.getInt(
                                            "total_obtained"
                                    ),
                                    resultSet.getInt(
                                            "total_possible"
                                    ),
                                    percentage,
                                    gradeResult.grade(),
                                    gradeResult.teacherRemark()
                            );

                    rankedStudents.put(
                            studentRank.edNo(),
                            studentRank
                    );
                }
            }

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Unable to calculate ranking for class: "
                            + className,
                    exception
            );
        }

        return rankedStudents;
    }

    public void dialogWhileGeneration(
            String title,
            String message
    ) {
        DialogPane dialogPane =
                new DialogPane();

        DialogPane.Dialog<ButtonType> dialog =
                new DialogPane.Dialog<>(
                        dialogPane,
                        DialogPane.Type.WARNING
                );

        dialog.setTitle(title);
        dialog.setContentAlignment(Pos.CENTER);

        dialogContent.setSpacing(10);

        generation.setMaxWidth(
                Double.MAX_VALUE
        );

        generation.setMinHeight(10);

        VBox.setVgrow(
                listOfGenerated,
                Priority.ALWAYS
        );

        dialogContent.getChildren().setAll(
                new Label(message),
                generation,
                listOfGenerated
        );

        dialogContent.setPrefSize(
                700,
                600
        );

        dialogContent.setMaxSize(
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        dialog.setContent(dialogContent);

        mainController.stackPane
                .getChildren()
                .add(dialogPane);

        dialog.show();
    }

    private void showError(
            String title,
            String message
    ) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() ->
                    showError(title, message)
            );

            return;
        }

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(
                message == null || message.isBlank()
                        ? "An unknown error occurred."
                        : message
        );

        alert.show();
    }
}