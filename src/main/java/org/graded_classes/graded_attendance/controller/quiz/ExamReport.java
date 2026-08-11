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
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
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
import org.graded_classes.graded_attendance.data.Student;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class ExamReport implements Initializable {
    MainController mainController;
    @FXML
    private Text avatar;
    int count = 0;
    @FXML
    private Label class_of, grade,
            marks_obtain, name_class,
            remarks, roll_no, total_marks,
            overAllRemark, totalRemark, rank;
    @FXML
    private SVGImageView icons;
    @FXML
    RingProgressIndicator allSubjectPercentage;
    @FXML
    private VBox report;
    @FXML
    private VBox mathResult;
    @FXML
    private VBox physicsResult;
    @FXML
    private VBox biologyResult;
    @FXML
    HBox performanceLayer;

    @FXML
    private VBox chemistryResult, englishResult;

    @FXML
    private SearchField<String> search;

    public ExamReport(MainController mainController) {
        this.mainController = mainController;
    }

    ObservableList<String> studentData = FXCollections.observableArrayList(List.of());
    LinkedHashMap<String, Student> data;
    VBox dialogContent = new VBox();
    ProgressBar generation = new ProgressBar();
    ScrollPane listOfGenerated = new ScrollPane();
    VBox sentItems = new VBox();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        icons.setSvgUrl(GradedResourceLoader.load("icons/my-logo.svg"));
        data = mainController.gradedDataLoader.getStudentData();
        studentData.addAll(asList(data.values()));
        search.setSuggestionProvider(request ->
                studentData.stream().filter(country ->
                                country.toLowerCase().contains(request.getUserText().toLowerCase())).
                        collect(Collectors.toList()));
        search.setOnCommit(this::generateReport);
        dialogContent.getChildren().add(generation);
        dialogContent.getChildren().add(listOfGenerated);
    }

    private int generateReport(String s) {
        if (s == null || s.isBlank()) {
            return -1;
        }
        report.setVisible(true);
        System.out.println(s);
        String roll = s.trim().split("\\s+", 2)[0];
        String name = s.trim().split("\\s+", 2)[1];
        var resultList = getClassRank(data.get(roll)._class());
        String abr = getNameAbbreviation(name);
        String sql = """
                SELECT 
                    subject,
                    COUNT(*) AS exam_count,
                    SUM(marks_obtain) AS total_obtained,
                    SUM(total_marks) AS total_possible,
                    AVG(marks_obtain) AS average_marks
                FROM ScoreCard
                WHERE ed_no = ?
                GROUP BY subject
                ORDER BY subject
                """;

        var connection = mainController.gradedDataLoader.databaseLoader.getConnection();

        Map<String, SubjectScoreSummary> subjectWiseResult = new LinkedHashMap<>();

        try (var ps = connection.prepareStatement(sql)) {

            ps.setString(1, roll);

            try (var rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    String subject = rs.getString("subject");
                    int examCount = rs.getInt("exam_count");
                    int totalObtained = rs.getInt("total_obtained");
                    int totalPossible = rs.getInt("total_possible");
                    double averageMarks = rs.getDouble("average_marks");

                    double percentage = totalPossible == 0
                            ? 0
                            : (totalObtained * 100.0) / totalPossible;

                    SubjectScoreSummary summary = new SubjectScoreSummary(
                            subject,
                            examCount,
                            totalObtained,
                            totalPossible,
                            averageMarks,
                            percentage
                    );

                    subjectWiseResult.put(subject, summary);
                }

                if (!found) {
                    System.out.println("No score card found for roll number: " + roll);
                    report.setVisible(false);
                    return -1;
                }
                avatar.setText(abr);
                name_class.setText(name);
                roll_no.setText("Roll No. " + roll);
                class_of.setText("Class " + data.get(roll)._class());
                double overallOfAllSubject = 0.0;
                double overallOfAllTotalMarks = 0.0;
                for (var entry : subjectWiseResult.entrySet()) {
                    SubjectScoreSummary summary = entry.getValue();
                    switch (summary.subject()) {
                        case "Math" -> {
                            plotData(mathResult, summary.percentage / 100,
                                    summary.totalObtained + "/" + summary.totalPossible(),
                                    getGrade(summary.percentage).grade());
                        }
                        case "Chemistry" -> {
                            plotData(chemistryResult, summary.percentage / 100,
                                    summary.totalObtained + "/" + summary.totalPossible(),
                                    getGrade(summary.percentage).grade());
                        }
                        case "Physics" -> {
                            plotData(physicsResult, summary.percentage / 100,
                                    summary.totalObtained + "/" + summary.totalPossible(),
                                    getGrade(summary.percentage).grade());
                        }
                        case "Biology" -> {
                            plotData(biologyResult, summary.percentage / 100,
                                    summary.totalObtained + "/" + summary.totalPossible(),
                                    getGrade(summary.percentage).grade());
                        }
                        case "English" -> {
                            plotData(englishResult, summary.percentage / 100,
                                    summary.totalObtained + "/" + summary.totalPossible(),
                                    getGrade(summary.percentage).grade());
                        }

                    }
                    removeIfNotNeed(subjectWiseResult.keySet());
                    overallOfAllSubject += summary.averageMarks;
                    overallOfAllTotalMarks += summary.totalPossible();
                }
                allSubjectPercentage.setProgress(overallOfAllSubject / overallOfAllTotalMarks);
                total_marks.setText(" / " + overallOfAllTotalMarks);
                marks_obtain.setText("" + overallOfAllSubject);
                grade.setText(getGrade(overallOfAllSubject / overallOfAllTotalMarks * 100).grade());
                rank.setText(resultList.containsKey(roll) ? resultList.get(roll).rank() + "" : "None");
                overAllRemark.setText(getGrade(overallOfAllSubject / overallOfAllTotalMarks * 100).remark());
                totalRemark.setText(getGrade(overallOfAllSubject / overallOfAllTotalMarks * 100).remark());
                remarks.setText(getGrade(overallOfAllSubject / overallOfAllTotalMarks * 100).teacherRemark());

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    private void removeIfNotNeed(Set<String> strings) {
        if (!strings.contains(physicsResult.getId()))
            performanceLayer.getChildren().remove(physicsResult);
        if (!strings.contains(mathResult.getId()))
            performanceLayer.getChildren().remove(mathResult);
        if (!strings.contains(chemistryResult.getId()))
            performanceLayer.getChildren().remove(chemistryResult);
        if (!strings.contains(englishResult.getId()))
            performanceLayer.getChildren().remove(englishResult);
        if (!strings.contains(biologyResult.getId()))
            performanceLayer.getChildren().remove(biologyResult);
    }


    private void plotData(VBox box, double p, String ma, String gr) {
        RingProgressIndicator per = (RingProgressIndicator) box.lookup("#percentage");
        Label mark = (Label) box.lookup("#marks");
        Label grade = (Label) box.lookup("#garde");
        per.setProgress(p);
        mark.setText(ma);
        grade.setText(gr);
    }

    private String getNameAbbreviation(String name) {
        var split = name.split(" ");
        return split.length > 1 ? "" + split[0].charAt(0) + split[split.length - 1].charAt(0) :
                (split[0].length() > 2 ? split[0].substring(0, 2) : split[0]);
    }

    private List<String> asList(Collection<Student> values) {
        List<String> result = new ArrayList<>();
        for (Student student : values) {
            studentData.add(student.ed_no() + " " + student.name());
        }
        return result;
    }

    @FXML
    void sendAllReport(ActionEvent event) {
        listOfGenerated.setContent(sentItems);
        generation.progressProperty().unbind();
        generation.setProgress(0);
        Button button = (Button) event.getSource();
        button.setDisable(true);

        dialogWhileGeneration();

        File reportFolder =
                new File(Main.getRootPath() + "/My Drive/StudentReport/");

        CompletableFuture.runAsync(() -> {

            int total = data.size();
            int current = 0;

            for (var entry : data.entrySet()) {

                String roll = entry.getKey();
                Student student = entry.getValue();

                try {

                    String telegramId = student.telegram_id();

                    if (telegramId != null && !telegramId.isBlank()) {

                        File pdf =
                                new File(reportFolder,
                                        roll + "_report.pdf");

                        if (pdf.exists()) {

                            mainController.messageSender.sendDocument(
                                    pdf,
                                    Long.parseLong(telegramId),"progress report"
                            );

                            Platform.runLater(() -> {
                                sentItems.getChildren().add(
                                        new Label(
                                                "✅ Sent : "
                                                        + student.name()
                                                        + " (" + roll + ")"
                                        )
                                );
                            });

                        } else {

                            Platform.runLater(() -> {
                                sentItems.getChildren().add(
                                        new Label(
                                                "❌ PDF Not Found : "
                                                        + student.name()
                                        )
                                );
                            });
                        }
                    } else {

                        Platform.runLater(() -> {
                            sentItems.getChildren().add(
                                    new Label(
                                            "⚠ No Telegram ID : "
                                                    + student.name()
                                    )
                            );
                        });
                    }

                    // Telegram rate limiting protection
                    Thread.sleep(1000);

                } catch (Exception ex) {

                    Platform.runLater(() -> {
                        sentItems.getChildren().add(
                                new Label(
                                        "❌ Failed : "
                                                + student.name()
                                                + " -> "
                                                + ex.getMessage()
                                )
                        );
                    });

                    ex.printStackTrace();
                }

                current++;

                double progress = (double) current / total;

                Platform.runLater(() ->
                        generation.setProgress(progress)
                );
            }

        }).whenComplete((r, ex) -> {

            Platform.runLater(() -> {

                button.setDisable(false);

                generation.setProgress(1);

                Alert alert = new Alert(
                        Alert.AlertType.INFORMATION,
                        "All reports processed."
                );

                alert.show();
            });
        });
    }

    @FXML
    void generateReport(ActionEvent event) {

        File file = new File(Main.getRootPath() + "/My Drive/StudentReport/");
        file.mkdirs();

        Button bt = (Button) event.getSource();
        bt.setDisable(true);

        dialogWhileGeneration();

        Task<Void> reportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {

                int total = data.size();
                int current = 0;

                for (var x : data.keySet()) {

                    String student = x + " " + data.get(x).name();

                    CountDownLatch latch = new CountDownLatch(1);

                    Platform.runLater(() -> {
                        try {
                            int result = generateReport(student);

                            if (result == 1) {
                                savePaneAsPdf(
                                        report,
                                        file.getAbsolutePath() + "/" + x + "_report.pdf"
                                );
                            }
                        } finally {
                            latch.countDown();
                        }
                    });

                    latch.await();

                    current++;

                    updateProgress(current, total);
                    updateMessage(current + " / " + total);
                }

                return null;
            }
        };

        generation.progressProperty().bind(reportTask.progressProperty());

        reportTask.messageProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() ->
                    listOfGenerated.setContent(new Label(newVal))
            );
        });

        reportTask.setOnSucceeded(e -> {
            bt.setDisable(false);
            System.out.println("Completed");
        });

        reportTask.setOnFailed(e -> {
            bt.setDisable(false);
            reportTask.getException().printStackTrace();
        });

        Thread thread = new Thread(reportTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void savePaneAsPdf(
            VBox pane,
            String outputFile
    ) {

        try {

            SnapshotParameters params =
                    new SnapshotParameters();

            params.setTransform(new Scale(3, 3));
            params.setFill(Color.WHITE);
            PDDocument document =
                    new PDDocument();

            WritableImage image = pane.snapshot(params, null);


            PDPage page = new PDPage(
                    new PDRectangle((float) image.getWidth(), (float) image.getHeight())
            );

            document.addPage(page);

            PDImageXObject pdfImage =
                    LosslessFactory.createFromImage(
                            document,
                            SwingFXUtils.fromFXImage(
                                    image,
                                    null
                            )
                    );

            PDPageContentStream contentStream =
                    new PDPageContentStream(
                            document,
                            page
                    );
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            float imageWidth = pdfImage.getWidth();
            float imageHeight = pdfImage.getHeight();

            float scale = Math.min(
                    pageWidth / imageWidth,
                    pageHeight / imageHeight
            );

            float drawWidth = imageWidth * scale;
            float drawHeight = imageHeight * scale;

            float x = (pageWidth - drawWidth) / 2;
            float y = (pageHeight - drawHeight) / 2;

            contentStream.drawImage(
                    pdfImage,
                    x,
                    pageHeight - drawHeight,
                    drawWidth,
                    drawHeight
            );

            contentStream.close();

            document.save(outputFile);
            document.close();

            System.out.println("High quality PDF saved: " + outputFile);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save PDF", e);
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

    public record GradeResult(String grade, String remark, String teacherRemark) {
    }

    public GradeResult getGrade(double percentage) {
        if (Double.isNaN(percentage)
                || Double.isInfinite(percentage)
                || percentage < 0
                || percentage > 100) {

            throw new IllegalArgumentException(
                    "Percentage must be between 0 and 100"
            );
        }

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

    public LinkedHashMap<String, StudentRank> getClassRank(String className) {

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
                            WHEN COALESCE(SUM(sc.total_marks), 0) = 0 THEN 0
                            ELSE COALESCE(SUM(sc.marks_obtain), 0) * 100.0
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

        LinkedHashMap<String, StudentRank> rankedStudents = new LinkedHashMap<>();

        var connection =
                mainController.gradedDataLoader.databaseLoader.getConnection();

        try (var ps = connection.prepareStatement(sql)) {

            ps.setString(1, className);

            try (var rs = ps.executeQuery()) {

                while (rs.next()) {

                    double percentage = rs.getDouble("percentage");
                    GradeResult gradeResult = getGrade(percentage);

                    StudentRank studentRank = new StudentRank(
                            rs.getInt("student_rank"),
                            rs.getString("ed_no"),
                            rs.getString("name"),
                            rs.getString("student_class"),
                            rs.getInt("exams_count"),
                            rs.getInt("total_obtained"),
                            rs.getInt("total_possible"),
                            percentage,
                            gradeResult.grade(),
                            gradeResult.teacherRemark()
                    );

                    rankedStudents.put(rs.getString("ed_no"), studentRank);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Unable to calculate ranking for class: " + className,
                    e
            );
        }

        return rankedStudents;
    }


    public void dialogWhileGeneration() {
        com.dlsc.gemsfx.DialogPane dialogPane = new com.dlsc.gemsfx.DialogPane();
        com.dlsc.gemsfx.DialogPane.Dialog<ButtonType> dialog = new com.dlsc.gemsfx.DialogPane.Dialog<>(dialogPane, DialogPane.Type.WARNING);
        dialog.setTitle("Report generation progress");
        dialog.setContentAlignment(Pos.CENTER);
        dialogContent.setSpacing(10);

        generation.setMaxWidth(Double.MAX_VALUE);

        VBox.setVgrow(listOfGenerated, Priority.ALWAYS);
        generation.setMinHeight(15);
        dialogContent.getChildren().setAll(
                new Label("Generating reports..."),
                generation,
                listOfGenerated
        );

        dialogContent.setPrefSize(700, 600);
        dialogContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        dialog.setContent(dialogContent);
        mainController.stackPane.getChildren().add(dialogPane);
        dialog.show();
    }

}