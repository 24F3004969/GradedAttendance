package org.graded_classes.graded_attendance.controller.quiz;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ExamReport implements Initializable {
    MainController mainController;
    @FXML
    private VBox report;
    @FXML
    private SearchField<String> search;
    public ExamReport(MainController mainController){
        this.mainController=mainController;
    }
    ObservableList<String> studentData = FXCollections.observableArrayList(List.of());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        icons.setSvgUrl(GradedResourceLoader.load("icons/my-logo.svg"));
        studentData.addAll(asList(mainController.gradedDataLoader.getStudentData().values()));
        search.setSuggestionProvider(request ->
                studentData.stream().filter(country ->
                                country.toLowerCase().contains(request.getUserText().toLowerCase())).
                        collect(Collectors.toList()));
    }
    private List<String> asList(Collection<Student> values) {
        List<String> result = new ArrayList<>();
        for (Student student : values) {
            studentData.add(student.ed_no() + " " + student.name());
        }
        return result;
    }
    @FXML
    private Text ed;

    @FXML
    private SVGImageView icons;

    @FXML
    private Label id_user;

    @FXML
    private Label name_class;

    @FXML
    private Label recent_message;

    @FXML
    private Label recent_message1;

    @FXML
    private Label today;
    @FXML
    void sendAllReport(ActionEvent event) {

    }
    @FXML
    void generateReport(ActionEvent event) {
         savePaneAsPdf(report,"report.pdf");
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
}