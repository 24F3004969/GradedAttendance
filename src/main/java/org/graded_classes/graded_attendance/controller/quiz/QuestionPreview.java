package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.components.LatexView;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

public class QuestionPreview implements Initializable {

    String imagePath;
    String latexQuestion;
    ArrayList<Node> optionList;

    public QuestionPreview(String imagePath, String latexQuestion, ArrayList<Node> optionList) {
        this.imagePath = imagePath;
        this.latexQuestion = latexQuestion;
        this.optionList = optionList;
    }

    @FXML
    private VBox optionBox;

    @FXML
    private ImageView questionImage;

    @FXML
    private LatexView questionLatexView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionImage.setImage(new Image(new File(imagePath).toURI().toString()));
        questionLatexView.setFormula("""
                \\[
                \\text{Two identical long current-carrying wires \\\\are bent into the shapes shown in the figure.}
                \\]
                \\\\
                \\[
                \\text{If the magnitude of magnetic fields at the centres } P\s
                \\text{ and } Q \\\\ \\text{ of the semicircular arc are } B_1\s
                \\text{ and } B_2,\\text{ find }
                \\frac{B_1}{B_2}.
                \\]
                """);

        for (var option : optionList) {
            optionBox.getChildren().add(option);
        }
    }
}
