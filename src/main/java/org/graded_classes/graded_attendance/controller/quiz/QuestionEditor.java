package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.SegmentedControl;
import atlantafx.base.controls.ToggleLabel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class QuestionEditor implements Initializable {
    private int questionCount = 0;
    ArrayList<QuestionEditorModel> listOfQuestions = new ArrayList<>();
    MainController mainController;
    TreeMap<Integer, QuestionData> integerQuestionDataTreeMap;
    ToggleLabel edit = new ToggleLabel("Edit");
    ToggleLabel preview = new ToggleLabel("Preview");
    Integer maxQuestionCount;
    String topicID;

    public QuestionEditor(MainController mainController, Integer maxQuestionCount, String topicID) {
        this.mainController = mainController;
        this.maxQuestionCount = maxQuestionCount;
        this.topicID = topicID;
        System.out.println(this.topicID);
    }

    public QuestionEditor(MainController mainController, TreeMap<Integer, QuestionData> integerQuestionDataTreeMap, String topicID) {
        this.mainController = mainController;
        this.maxQuestionCount = integerQuestionDataTreeMap.size();
        this.integerQuestionDataTreeMap = integerQuestionDataTreeMap;
        this.topicID = topicID;
    }

    @FXML
    private ScrollPane editor;

    @FXML
    private SegmentedControl segmentControl;

    @FXML
    private Label status;
    @FXML
    Button saveOrEditButton;

    @FXML
    void onBasicAction(ActionEvent event) {
        System.out.println(maxQuestionCount + "   " + questionCount);
        String buttonText = ((Button) event.getSource()).getText();
        int totalQuestionNumber = listOfQuestions.size() - 1;
        var questionSet = integerQuestionDataTreeMap == null ? null : integerQuestionDataTreeMap.sequencedValues().stream().toList();
        switch (buttonText) {
            case "Next" -> {
                if (questionCount == totalQuestionNumber && questionCount <= maxQuestionCount - 2) {
                    questionCount = totalQuestionNumber + 1;
                    Questions controller;
                    controller = new Questions(mainController, "Question " + (questionCount + 1), questionSet == null
                            ? null : questionSet.get(questionCount));
                    var content = (VBox) mainController.gradedFxmlLoader.createView(R.question, controller);
                    listOfQuestions.add(new QuestionEditorModel(content, controller, segmentControl.getSegments().getFirst()));
                    if (segmentControl.getToggleGroup().getSelectedToggle() instanceof ToggleLabel l) {
                        if (l.getText().equals("Edit")) {
                            edit(l);
                        } else if (l.getText().equals("Preview")) {
                            preview(l);
                        }
                    }
                } else {
                    if (questionCount <= totalQuestionNumber && questionCount <= maxQuestionCount - 2) {
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
        segmentControl.getSegments().add(edit);
        segmentControl.getSegments().add(preview);
        Questions controller;
        if (integerQuestionDataTreeMap != null) {
            controller = new Questions(mainController, "Question " + (questionCount + 1),
                    integerQuestionDataTreeMap.sequencedValues().getFirst());
            list = integerQuestionDataTreeMap.sequencedValues().stream().toList();
            saveOrEditButton.setText("Edit");
        } else
            controller = new Questions(mainController, "Question " + (questionCount + 1));
        var content = (VBox) mainController.gradedFxmlLoader.createView(R.question, controller);
        listOfQuestions.add(new QuestionEditorModel(content, controller, segmentControl.getSegments().getFirst()));
        segmentControl.getToggleGroup().selectedToggleProperty().subscribe(toggle -> {
            if (toggle instanceof ToggleLabel l) {
                if (l.getText().equals("Edit")) {
                    edit(l);
                } else if (l.getText().equals("Preview")) {
                  /*  if (list != null)
                        preview(l);
                    else*/
                    preview(listOfQuestions.get(questionCount).question);
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
        List<String> list1 = data.option_data().options().values().stream().toList();
        editor.setContent(mainController.gradedFxmlLoader.createView(R.question_preview,
                new QuestionPreview((data.question_img_path() == null || data.question_img_path().isEmpty()) ? "" : data.question_img_path(),
                        "\\text{%s}".formatted(data.question_txt()), new ArrayList<>(List.of(
                        mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("A)",
                                "\\text{%s}".formatted(list1.getFirst()), "", toggleGroup)),
                        mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("B)",
                                "\\text{%s}".formatted(list1.get(1)), "", toggleGroup)),
                        mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("C)",
                                "\\text{%s}".formatted(list1.get(2)), "", toggleGroup)),
                        mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("D)",
                                "\\text{%s}".formatted(list1.get(3)), "", toggleGroup))
                )))));
    }

    private void preview(Questions data) {
        var toggleGroup = new ToggleGroup();
        editor.setContent(mainController.gradedFxmlLoader.createView(R.question_preview,
                new QuestionPreview(data.file == null ? "" : data.file,
                        "\\text{%s}".formatted(data.question_text.getText()), new ArrayList<>(List.of(
                        mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("A)",
                                "\\text{%s}".formatted(data.opt1.getText()), "", toggleGroup)),
                        mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("B)",
                                "\\text{%s}".formatted(data.opt2.getText()), "", toggleGroup)),
                        mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("C)",
                                "\\text{%s}".formatted(data.opt3.getText()), "", toggleGroup)),
                        mainController.gradedFxmlLoader.createView(R.question_option, new QuestionOption("D)",
                                "\\text{%s}".formatted(data.opt4.getText()), "", toggleGroup))
                )))));
    }

    public void onEditOrSave() {
        if (saveOrEditButton.getText().equals("Save")) {
            saveData();
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("Question got save");
            alert.show();
        } else if (saveOrEditButton.getText().equals("Edit")) {
            editData();
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("Question got edited");
            alert.show();
        }
    }

    private void saveData() {
        if (listOfQuestions.isEmpty()) {
            return;
        }
        try {
            var conn = mainController.gradedDataLoader.databaseLoader.getConnection();

            String questionSql = """
                    INSERT INTO Questions
                    (topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
                    VALUES (?,?,?,?,?,?,?)
                    """;

            String optionSql = """
                    INSERT INTO QuestionOptions
                    (question_id, option_text, option_img_path, option_order, is_correct)
                    VALUES (?,?,?,?,?)
                    """;

            for (var x : listOfQuestions) {
                if (x.question.file != null) {
                    String des = Main.getRootPath() + "My Drive/imageData/" + x.question.getFile().getName();
                    copyFile(x.question.file, des);
                    x.question.file = des;
                }
                Toggle selectedToggle = x.question.t_options.getSelectedToggle();

                if (selectedToggle == null) {
                    throw new RuntimeException("Please select question level.");
                }

                int questionId;

                try (PreparedStatement pStat = conn.prepareStatement(questionSql, Statement.RETURN_GENERATED_KEYS)) {
                    pStat.setInt(1, Integer.parseInt(topicID));
                    pStat.setInt(2, 1);
                    pStat.setString(3, LocalDate.now().toString());
                    pStat.setString(4, "mcq");
                    pStat.setString(5, ((RadioButton) selectedToggle).getText());
                    pStat.setString(6, x.question.question_text.getText());
                    pStat.setString(7, x.question.file);

                    pStat.executeUpdate();

                    try (ResultSet rs = pStat.getGeneratedKeys()) {
                        if (rs.next()) {
                            questionId = rs.getInt(1);
                        } else {
                            throw new RuntimeException("Question ID was not generated.");
                        }
                    }
                }

                try (PreparedStatement optionStmt = conn.prepareStatement(optionSql)) {
                    insertOption(optionStmt, questionId, x.question.opt1.getText(), 1, x.question.cOp1.isSelected());
                    insertOption(optionStmt, questionId, x.question.opt2.getText(), 2, x.question.cOp2.isSelected());
                    insertOption(optionStmt, questionId, x.question.opt3.getText(), 3, x.question.cOp3.isSelected());
                    insertOption(optionStmt, questionId, x.question.opt4.getText(), 4, x.question.cOp4.isSelected());
                }
            }

            if (!conn.getAutoCommit()) {
                conn.commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void editData() {
        try {
            var conn = mainController.gradedDataLoader.databaseLoader.getConnection();

            QuestionData qd = list.get(questionCount);
            int questionId = Integer.parseInt(qd.question_id());
            List<Integer> list1 = qd.option_data().options().keySet().stream().toList();

            var x = listOfQuestions.get(questionCount);

            if (x.question.file != null && new File(x.question.file.toString()).exists()) {
                String des = Main.getRootPath() +
                        "My Drive/imageData/" +
                        x.question.getFile().getName();

                copyFile(x.question.file, des);
                x.question.file = des;
            }

            Toggle selectedToggle = x.question.t_options.getSelectedToggle();

            if (selectedToggle == null) {
                throw new RuntimeException("Please select question level.");
            }

            String updateQuestionSql = """
                    UPDATE Questions
                    SET level = ?,
                        question_txt = ?,
                        question_img_path = ?
                    WHERE question_id = ?
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(updateQuestionSql)) {

                stmt.setString(1, ((RadioButton) selectedToggle).getText());
                stmt.setString(2, x.question.question_text.getText());
                stmt.setString(3, x.question.file);
                stmt.setInt(4, questionId);

                stmt.executeUpdate();
            }
            String updateOptionSql = """
                    UPDATE QuestionOptions
                    SET option_text = ?,
                        option_img_path = ?,
                        is_correct = ?
                    WHERE option_id = ?
                    """;
            try (PreparedStatement optionStmt = conn.prepareStatement(updateOptionSql)) {

                updateOption(optionStmt, x.question.opt1.getText(), null,
                        x.question.cOp1.isSelected(), list1.getFirst());

                updateOption(optionStmt, x.question.opt2.getText(), null,
                        x.question.cOp2.isSelected(), list1.get(1));

                updateOption(optionStmt, x.question.opt3.getText(), null,
                        x.question.cOp3.isSelected(), list1.get(2));

                updateOption(optionStmt, x.question.opt4.getText(), null,
                        x.question.cOp4.isSelected(), list1.get(3));

                optionStmt.executeBatch();
            }
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            qd.option_data().options().replace(0, x.question.opt1.getText());
            qd.option_data().options().replace(1, x.question.opt2.getText());
            qd.option_data().options().replace(2, x.question.opt3.getText());
            qd.option_data().options().replace(3, x.question.opt4.getText());
            qd.setQuestion_txt(x.question.question_text.getText());
            if (x.question.cOp1.isSelected())
                qd.option_data().setOption_index(0);
            else if (x.question.cOp2.isSelected())
                qd.option_data().setOption_index(1);
            else if (x.question.cOp3.isSelected())
                qd.option_data().setOption_index(3);
            else if (x.question.cOp4.isSelected())
                qd.option_data().setOption_index(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertOption(
            PreparedStatement optionStmt,
            int questionId,
            String optionText,
            int optionOrder,
            boolean isCorrect
    ) throws SQLException {

        optionStmt.setInt(1, questionId);
        optionStmt.setString(2, optionText);
        optionStmt.setString(3, "");
        optionStmt.setInt(4, optionOrder);
        optionStmt.setInt(5, isCorrect ? 1 : 0);

        optionStmt.executeUpdate();
    }

    private void updateOption(
            PreparedStatement stmt,
            String optionText,
            String optionImagePath,
            boolean isCorrect,
            int optionId) throws SQLException {

        stmt.setString(1, optionText);
        stmt.setString(2, optionImagePath);
        stmt.setBoolean(3, isCorrect);
        stmt.setInt(4, optionId);

        stmt.executeUpdate();

    }

    public static void copyFile(String sourcePath, String destinationPath) {
        try {
            Files.copy(
                    Path.of(sourcePath),
                    Path.of(destinationPath),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}