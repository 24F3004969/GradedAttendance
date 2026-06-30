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
import org.graded_classes.graded_attendance.data.QuestionData;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class QuestionEditor implements Initializable {
    private int questionCount = 0;
    ArrayList<QuestionEditorModel> listOfQuestions = new ArrayList<>();
    MainController mainController;
    TreeMap<Integer, QuestionData> integerQuestionDataTreeMap;

    public QuestionEditor(MainController mainController) {
        this.mainController = mainController;
    }

    public QuestionEditor(MainController mainController, TreeMap<Integer, QuestionData> integerQuestionDataTreeMap) {
        this.mainController = mainController;
        this.integerQuestionDataTreeMap = integerQuestionDataTreeMap;
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
        var questionSet = integerQuestionDataTreeMap.sequencedValues().stream().toList();
        switch (buttonText) {
            case "Next" -> {
                if (questionCount == totalQuestionNumber) {
                    questionCount = totalQuestionNumber + 1;
                    Questions controller;
                    /*if (!questionIdSet.isEmpty()) {
                        controller = new Questions(mainController,
                                "Question " + (questionCount + 1));
                    } else*/
                    controller = new Questions(mainController,
                            "Question " + (questionCount + 1), questionSet.get(questionCount));
                    var content = (VBox) mainController.gradedFxmlLoader.createView(R.question, controller);
                    editor.setContent(content);
                    listOfQuestions.add(new QuestionEditorModel(content, controller,
                            segmentControl.getSegments().getFirst()));
                } else {
                    if (questionCount <= totalQuestionNumber) {
                        questionCount++;
                        editor.setContent(listOfQuestions.get(questionCount).root);
                        segmentControl.getToggleGroup().selectToggle(listOfQuestions.
                                get(questionCount).status);
                    }
                }
            }
            case "Previous" -> {
                if (questionCount >= 1) {
                    questionCount--;
                    editor.setContent(listOfQuestions.get(questionCount).root);
                    segmentControl.getToggleGroup().selectToggle(listOfQuestions.get(questionCount).status);

                }
            }
            case "Save" -> {
                for (var editorModel : listOfQuestions) {
                    var q = new QuestionData("1", "1", "1", LocalDate.now().toString(),
                            "mcq", "medium", editorModel.question.getQuestion_text().getText(),
                            "", null);
                    System.out.println(q);
                }
                System.out.println("Saved");
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println(integerQuestionDataTreeMap);
        segmentControl.getSegments().add(new ToggleLabel("Edit"));
        segmentControl.getSegments().add(new ToggleLabel("Preview"));
        Questions controller = new Questions(mainController, "Question " + (questionCount + 1),
                integerQuestionDataTreeMap.sequencedValues().getFirst());
        var content = (VBox) mainController.gradedFxmlLoader.createView(R.question,
                controller);
        var list = integerQuestionDataTreeMap.sequencedValues().stream().toList();
        listOfQuestions.add(new QuestionEditorModel(content, controller, segmentControl.getSegments().getFirst()));
        segmentControl.getToggleGroup().selectedToggleProperty().subscribe(toggle -> {
            if (toggle instanceof ToggleLabel l) {
                if (l.getText().equals("Edit")) {
                    editor.setContent(listOfQuestions.get(questionCount).root);
                    listOfQuestions.get(questionCount).status = l;
                } else if (l.getText().equals("Preview")) {
                    var toggleGroup = new ToggleGroup();
                    var data = list.get(questionCount);
                    editor.setContent(mainController.gradedFxmlLoader.createView(R.question_preview,
                            new QuestionPreview("",
                                    "\\text{%s}".formatted(data.question_txt()), new ArrayList<>(List.of(
                                    mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("A)",
                                            "\\text{%s}".formatted(data.option_data().options().getFirst()), "", toggleGroup)),
                                    mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("B)",
                                            "\\text{%s}".formatted(data.option_data().options().get(1)), "", toggleGroup)),
                                    mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("C)",
                                            "\\text{%s}".formatted(data.option_data().options().get(2)), "", toggleGroup)),
                                    mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("D)",
                                            "\\text{%s}".formatted(data.option_data().options().get(3)), "", toggleGroup))
                            )))));
                    listOfQuestions.get(questionCount).status = l;
                }
            }
        });

    }

}