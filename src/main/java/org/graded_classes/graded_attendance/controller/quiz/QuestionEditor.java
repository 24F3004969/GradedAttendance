package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.SegmentedControl;
import atlantafx.base.controls.ToggleLabel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
    ToggleLabel edit = new ToggleLabel("Edit");
    ToggleLabel preview = new ToggleLabel("Preview");

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
                    controller = new Questions(mainController,
                            "Question " + (questionCount + 1), questionSet.get(questionCount));
                    var content = (VBox) mainController.gradedFxmlLoader.createView(R.question, controller);
                    listOfQuestions.add(new QuestionEditorModel(content, controller,
                            segmentControl.getSegments().getFirst()));
                    if (segmentControl.getToggleGroup().getSelectedToggle() instanceof ToggleLabel l) {
                        if (l.getText().equals("Edit")) {
                            edit(l);
                        } else if (l.getText().equals("Preview")) {
                            preview(l);
                        }
                    }
                } else {
                    if (questionCount <= totalQuestionNumber) {
                        questionCount++;
                        if (segmentControl.getToggleGroup().getSelectedToggle() instanceof ToggleLabel l) {
                            if (l.getText().equals("Edit")) {
                                edit(l);
                            } else if (l.getText().equals("Preview")) {
                                preview(l);
                            }
                        }
                    }
                }
            }
            case "Previous" -> {
                if (questionCount >= 1) {
                    questionCount--;
                    if (segmentControl.getToggleGroup().getSelectedToggle() instanceof ToggleLabel l) {
                        if (l.getText().equals("Edit")) {
                            edit(l);
                        } else if (l.getText().equals("Preview")) {
                            preview(l);
                        }
                    }
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

    List<QuestionData> list;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println(integerQuestionDataTreeMap);
        segmentControl.getSegments().add(edit);
        segmentControl.getSegments().add(preview);
        Questions controller;
        if (integerQuestionDataTreeMap != null) {
             controller = new Questions(mainController, "Question " + (questionCount + 1),
                    integerQuestionDataTreeMap.sequencedValues().getFirst());
            list = integerQuestionDataTreeMap.sequencedValues().stream().toList();
        }
        else
            controller = new Questions(mainController, "Question " + (questionCount + 1));
        var content = (VBox) mainController.gradedFxmlLoader.createView(R.question,
                controller);
        listOfQuestions.add(new QuestionEditorModel(content, controller, segmentControl.getSegments().getFirst()));
        segmentControl.getToggleGroup().selectedToggleProperty().subscribe(toggle -> {
            if (toggle instanceof ToggleLabel l) {
                if (l.getText().equals("Edit")) {
                    edit(l);
                } else if (l.getText().equals("Preview")) {
                    preview(l);
                }
            }
        });

    }

    private void edit(ToggleLabel l) {
        editor.setContent(listOfQuestions.get(questionCount).root);
    }

    private void preview(ToggleLabel l) {
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
    }

    public void onEditOrSave() {
    }
}