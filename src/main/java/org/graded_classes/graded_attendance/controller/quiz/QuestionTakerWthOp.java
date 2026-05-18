package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import org.graded_classes.graded_attendance.components.LatexView;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class QuestionTakerWthOp implements Initializable {


    @FXML
    private ToggleGroup ans;

    @FXML
    private LatexView opt1;

    @FXML
    private LatexView opt2;

    @FXML
    private LatexView opt3;

    @FXML
    private LatexView opt4;

    @FXML
    private LatexView questionText;
    QuestionData questionData;

    public QuestionTakerWthOp(QuestionData questionData) {
        this.questionData = questionData;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionText.setFormula("\\text{%s}".formatted(questionData.question_txt()));
        opt1.setFormula("\\text{%s}".formatted(questionData.option_data().options().getFirst()));
        opt2.setFormula("\\text{%s}".formatted(questionData.option_data().options().get(1)));
        opt3.setFormula("\\text{%s}".formatted(questionData.option_data().options().get(2)));
        opt4.setFormula("\\text{%s}".formatted(questionData.option_data().options().getLast()));

    }
}
