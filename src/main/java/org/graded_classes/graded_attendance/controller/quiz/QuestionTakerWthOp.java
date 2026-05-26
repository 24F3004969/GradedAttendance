package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.RadioButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.graded_classes.graded_attendance.components.LatexView;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.net.URL;
import java.util.*;

public class QuestionTakerWthOp implements Initializable {

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
    LinkedHashMap<QuestionData, ArrayList<Integer>> selectedOptions;

    public QuestionTakerWthOp(QuestionData questionData, LinkedHashMap<QuestionData, ArrayList<Integer>> selectedOptions) {
        this.questionData = questionData;
        this.selectedOptions = selectedOptions;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionText.setFormula("\\text{%s}".formatted(questionData.question_txt()));
        opt1.setFormula("\\text{%s}".formatted(questionData.option_data().options().getFirst()));
        opt2.setFormula("\\text{%s}".formatted(questionData.option_data().options().get(1)));
        opt3.setFormula("\\text{%s}".formatted(questionData.option_data().options().get(2)));
        opt4.setFormula("\\text{%s}".formatted(questionData.option_data().options().getLast()));

    }

    @FXML
    public void whenOptionClicked(MouseEvent mouseEvent) {
        HBox hBox = (HBox) mouseEvent.getSource();
        int optionId = Integer.parseInt(hBox.getId().replace("option", ""));
        RadioButton rd = (RadioButton) hBox.lookup("#radio");
        rd.setSelected(true);
       /* if (selectedOptions.containsKey(optionId)) {
            selectedOptions.get(optionId).add(questionData.option_data().options().get(optionId - 1));
        }*/
            selectedOptions.put(questionData, new ArrayList<>(List.of(optionId)));

    }
}
