package org.graded_classes.graded_attendance.controller.quiz;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import org.graded_classes.graded_attendance.components.LatexView;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class TestPreview implements Initializable {
    public Label questionNumber;
    public LatexView questionText;
    public ImageView imageView;
    public LatexView opt1, opt2, opt3, opt4;
    TreeMap<String, QuestionData> questionData;
    int questionIndex;

    public TestPreview(TreeMap<String, QuestionData> questionData) {
        this.questionData = questionData;
        questionIndex = 0;
    }

    public void onRadioClicked(ActionEvent actionEvent) {

    }

    public void whenOptionClicked(MouseEvent mouseEvent) {

    }

    String questionId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionId = questionData.firstKey();
        addQuestion(1);


    }

    void addQuestion(int direction) {
        QuestionData qd = questionData.get(questionId);
        if (qd != null) {
            questionIndex=questionIndex + direction;
            questionNumber.setText("Question : " + (questionIndex) + ", Question Id:" + qd.question_id());
            questionText.setFormula("\\text{%s}".formatted(qd.question_txt()));
            opt1.setFormula("\\text{%s}".formatted(qd.option_data().options().get(1)));
            opt2.setFormula("\\text{%s}".formatted(qd.option_data().options().get(2)));
            opt3.setFormula("\\text{%s}".formatted(qd.option_data().options().get(3)));
            opt4.setFormula("\\text{%s}".formatted(qd.option_data().options().get(4)));
        }
    }

    @FXML
    public void action(ActionEvent actionEvent) {
        Button button = (Button) actionEvent.getSource();
        if (button.getText().equals("Previous") && questionData.lowerKey(questionId) != null) {
            questionId = questionData.lowerKey(questionId);
            addQuestion(-1);
        } else if (button.getText().equals("Next") && questionData.higherKey(questionId) != null) {
            questionId = questionData.higherKey(questionId);
            addQuestion(+1);
        }
    }

    @FXML
    public void close(ActionEvent actionEvent) {

    }

    @FXML
    public void regenerate(ActionEvent actionEvent) {

    }
}
