package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.SegmentedControl;
import atlantafx.base.controls.ToggleLabel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class QuestionEditor implements Initializable {
    private int questionCount = 0;
    ArrayList<QuestionEditorModel> listOfQuestions = new ArrayList<>();
    @FXML
    MainController mainController;

    public QuestionEditor(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private ScrollPane editor;

    @FXML
    private SegmentedControl segmentControl;

    @FXML
    private Label status;

    @FXML
    void onBasicAction(ActionEvent event) {
        String buttonText = ((Button) event.getSource()).getText();
        int totalQuestionNumber = listOfQuestions.size() - 1;
        if (buttonText.equals("Next")) {
            if (questionCount == totalQuestionNumber) {
                questionCount = totalQuestionNumber + 1;
                Questions controller = new Questions(mainController,
                        "Question " + (questionCount + 1));
                var content = (VBox) mainController.gradedFxmlLoader.createView(R.question, controller);
                editor.setContent(content);
                listOfQuestions.add(new QuestionEditorModel(content, controller, segmentControl.getSegments().getFirst()));
            } else {
                if (questionCount <= totalQuestionNumber) {
                    questionCount++;
                    editor.setContent(listOfQuestions.get(questionCount).root);
                    segmentControl.getToggleGroup().selectToggle(listOfQuestions.get(questionCount).status);
                }
            }
        } else if (buttonText.equals("Previous")) {
            if (questionCount >= 1) {
                questionCount--;
                editor.setContent(listOfQuestions.get(questionCount).root);
                segmentControl.getToggleGroup().selectToggle(listOfQuestions.get(questionCount).status);

            }
        } else if (buttonText.equals("Save")) {
            for(var QE:listOfQuestions){
                var q=new QuestionData("1","1","1", LocalDate.now().toString(),
                        "mcq","medium",QE.question.getQuestion_text().getText(),"",null);
                System.out.println(q);
            }
            System.out.println("Saved");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        segmentControl.getSegments().add(new ToggleLabel("Edit"));
        segmentControl.getSegments().add(new ToggleLabel("Preview"));
        Questions controller = new Questions(mainController, "Question " + (questionCount + 1));
        var content = (VBox) mainController.gradedFxmlLoader.createView(R.question,
                controller);
        listOfQuestions.add(new QuestionEditorModel(content, controller, segmentControl.getSegments().getFirst()));
        segmentControl.getToggleGroup().selectedToggleProperty().subscribe(toggle -> {
            if (toggle instanceof ToggleLabel l) {
                if (l.getText().equals("Edit")) {
                    editor.setContent(listOfQuestions.get(questionCount).root);
                    listOfQuestions.get(questionCount).status = l;
                } else if (l.getText().equals("Preview")) {
                    var toggleGroup = new ToggleGroup();
                    editor.setContent(mainController.gradedFxmlLoader.createView(R.question_preview,
                            new QuestionPreview("C:\\Users\\hilal\\OneDrive\\Pictures\\Screenshots 1\\Screenshot 2026-04-30 144956.png",
                                    "", new ArrayList<>(List.of(
                                    mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("A)", """
                                             \\frac{\\pi + 1}{\\pi + 2} \\\\
                                            """, "", toggleGroup)),
                                    mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("B)", """
                                             \\frac{\\pi + 2}{\\pi + 1} \\\\
                                            """, "", toggleGroup)),
                                    mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("C)", """
                                              \\frac{\\pi}{\\pi + 1} \\\\
                                            """, "", toggleGroup)),
                                    mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("D)", """
                                              \\frac{\\pi + 2}{\\pi}
                                            """, "", toggleGroup))
                            )))));
                    listOfQuestions.get(questionCount).status = l;
                }
            }
        });

    }

}