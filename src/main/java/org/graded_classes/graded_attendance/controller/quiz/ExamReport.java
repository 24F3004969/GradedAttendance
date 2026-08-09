package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.RingProgressIndicator;
import com.dlsc.gemsfx.SVGImageView;
import com.dlsc.gemsfx.SearchField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
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

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ExamReport implements Initializable {
    MainController mainController;
    @FXML
    private Text avatar;

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
    private VBox chemistryResult, englishResult;

    @FXML
    private SearchField<String> search;

    public ExamReport(MainController mainController) {
        this.mainController = mainController;
    }

    ObservableList<String> studentData = FXCollections.observableArrayList(List.of());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        icons.setSvgUrl(GradedResourceLoader.load("icons/my-logo.svg"));
        LinkedHashMap<String, Student> data = mainController.gradedDataLoader.getStudentData();
        studentData.addAll(asList(data.values()));
        search.setSuggestionProvider(request ->
                studentData.stream().filter(country ->
                                country.toLowerCase().contains(request.getUserText().toLowerCase())).
                        collect(Collectors.toList()));
        search.setOnCommit(s -> {
            if (s == null || s.isBlank()) {
                return;
            }

            String roll = s.trim().split("\\s+", 2)[0];
            String name = s.trim().split("\\s+", 2)[1];
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
                        return;
                    }
                    report.setVisible(true);
                    avatar.setText(abr);
                    name_class.setText(name);
                    roll_no.setText("Roll No. " + roll);
                    class_of.setText("Class " + data.get(roll)._class());
                    double overallOfAllSubject = 0.0;
                    double overallOfAllTotalMarks = 0.0;
                    for (var entry : subjectWiseResult.entrySet()) {
                        SubjectScoreSummary summary = entry.getValue();
                        System.out.println(summary.averageMarks);
                        switch (summary.subject()) {
                            case "Math" -> {
                                plotData(mathResult, summary.percentage/100,
                                        summary.totalObtained + "/" + summary.totalPossible(),
                                        getGrade(summary.percentage ).grade());
                            }
                            case "Chemistry" -> {
                                plotData(chemistryResult, summary.percentage/100,
                                        summary.totalObtained + "/" + summary.totalPossible(),
                                        getGrade(summary.percentage ).grade());
                            }
                            case "Physics" -> {
                                plotData(physicsResult, summary.percentage/100,
                                        summary.totalObtained + "/" + summary.totalPossible(),
                                        getGrade(summary.percentage ).grade());
                            }
                            case "Biology" -> {
                                plotData(biologyResult,summary.percentage/100,
                                        summary.totalObtained + "/" + summary.totalPossible(),
                                        getGrade(summary.percentage).grade());
                            }
                            case "English" -> {
                                plotData(englishResult,summary.percentage/100,
                                        summary.totalObtained + "/" + summary.totalPossible(),
                                        getGrade(summary.percentage).grade());
                            }
                        }
                        overallOfAllSubject += summary.averageMarks;
                        overallOfAllTotalMarks += summary.totalPossible();
                    }
                    allSubjectPercentage.setProgress(overallOfAllSubject / overallOfAllTotalMarks);
                    total_marks.setText(" / " + overallOfAllTotalMarks);
                    marks_obtain.setText("" + overallOfAllSubject);
                    grade.setText(getGrade(overallOfAllSubject / overallOfAllTotalMarks * 100).grade());
                    overAllRemark.setText(getGrade(overallOfAllSubject / overallOfAllTotalMarks * 100).remark());
                    totalRemark.setText(getGrade(overallOfAllSubject / overallOfAllTotalMarks * 100).remark());
                    remarks.setText(getGrade(overallOfAllSubject / overallOfAllTotalMarks * 100).teacherRemark());

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
        return split.length > 1 ? "" + split[0].charAt(0) + split[1].charAt(0) :
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

    }

    @FXML
    void generateReport(ActionEvent event) {
        savePaneAsPdf(report, "report.pdf");
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

}