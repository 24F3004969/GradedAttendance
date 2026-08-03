package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.CustomTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.QuestionData;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Questions implements Initializable {
    @FXML
    StackPane drag_area;
    @FXML
    Label QNum;
    @FXML
    CustomTextField opt1, opt2, opt3, opt4;

    @FXML
    ToggleGroup q_options, t_options;
    @FXML
    CheckBox cOp1, cOp2, cOp3, cOp4;
    @FXML
    Label file_name;
    @FXML
    CustomTextField question_text;
    String file;
    MainController mainController;
    String questionNumber;
    QuestionData questionData;

    public CustomTextField getQuestion_text() {
        return question_text;
    }

    public Questions(MainController mainController, String questionNumber) {
        this.mainController = mainController;
        this.questionNumber = questionNumber;
    }

    public Questions(MainController mainController, String questionNumber,
                     QuestionData questionData) {
        this.mainController = mainController;
        this.questionNumber = questionNumber;
        this.questionData = questionData;


    }

    @FXML
    void onBasicAction(ActionEvent event) {
        fileChooser(event);
    }

    public File getFile() {
        return new File(file);
    }

    @FXML
    public void handleDragOver(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        if (dragboard.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
        file = dragboard.getFiles().toString().substring(1, dragboard.getFiles().toString().length() - 1);

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        QNum.setText(questionNumber);
        question_text.setRight(new FontIcon("mdi2a-arrow-expand"));
        question_text.getRight().setCursor(Cursor.DEFAULT);
        question_text.getRight().setOnMouseClicked(event -> {
            var editor = mainController.gradedFxmlLoader.createView(R.latex_editor, new LatexEditor(mainController));
            mainController.modalPane.show(editor);
        });
        opt1.setRight(new FontIcon("mdi2a-arrow-expand"));
        opt2.setRight(new FontIcon("mdi2a-arrow-expand"));
        opt3.setRight(new FontIcon("mdi2a-arrow-expand"));
        opt4.setRight(new FontIcon("mdi2a-arrow-expand"));
        opt1.getRight().setCursor(Cursor.DEFAULT);
        opt2.getRight().setCursor(Cursor.DEFAULT);
        opt3.getRight().setCursor(Cursor.DEFAULT);
        opt4.getRight().setCursor(Cursor.DEFAULT);
        expand(opt1, opt2);
        expand(opt3, opt4);
        if (questionData != null) {
            List<String> list1 = questionData.option_data().options().values().stream().toList();
            selectCorrect();
            question_text.setText(questionData.question_txt());
            t_options.getToggles().get(getTypeId(questionData.level())).setSelected(true);
            opt1.setText(list1.getFirst());
            opt2.setText(list1.get(1));
            opt3.setText(list1.get(2));
            opt4.setText(list1.get(3));
        }
    }

    private void selectCorrect() {
        switch (questionData.option_data().option_index()) {
            case 0 -> cOp1.setSelected(true);
            case 1 -> cOp2.setSelected(true);
            case 2 -> cOp3.setSelected(true);
            case 3 -> cOp4.setSelected(true);
        }
    }

    private void expand(CustomTextField opt1, CustomTextField opt2) {
        opt1.getRight().setOnMouseClicked(event -> {
            var editor = mainController.gradedFxmlLoader.createView(R.latex_editor, new LatexEditor(mainController));
            mainController.modalPane.show(editor);
        });
        opt2.getRight().setOnMouseClicked(event -> {
            var editor = mainController.gradedFxmlLoader.createView(R.latex_editor, new LatexEditor(mainController));
            mainController.modalPane.show(editor);
        });
    }

    public int getTypeId(String level) {
        return switch (level) {
            case "Medium", "medium" -> 1;
            case "Hard", "very difficult" -> 2;
            default -> 0;
        };
    }

    public void handleDragDropped(DragEvent event) {
        file_name.setText(event.getDragboard().getFiles().toString());
        file_name.setVisible(true);
    }

    public void fileChooser(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Option Image Selector");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.jpg", "*.png"),
                new FileChooser.ExtensionFilter("Select JPEG", "*.jpg"),
                new FileChooser.ExtensionFilter("Select PNG", "*.png")
        );
        File file = fileChooser.showOpenDialog(mainController.getStage());
        if (file != null) {
            Button target = (Button) event.getSource();
            HBox parent = (HBox) target.getParent();
            Button delete = new Button();
            delete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            var icon = new FontIcon();

            icon.getStyleClass().add("ikonli-font-icon");

            icon.setCursor(Cursor.DEFAULT);
            icon.setStyle("""
                    -fx-icon-code:mdi2d-delete;
                    -fx-icon-color:red;
                    """);
            delete.setGraphic(icon);
            target.setText(file.getName());
            target.setDisable(true);
            parent.setSpacing(5);
            parent.getChildren().add(delete);
            delete.setOnMouseClicked(mouseEvent -> {
                parent.getChildren().remove(delete);
                target.setDisable(false);
                target.setText("Add Image");
            });
        }
        //imageView.setSvgUrl(file.toURI().toString());
    }
}
