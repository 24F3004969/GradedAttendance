package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.CustomTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.data.QuestionData;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Questions implements Initializable {
    @FXML
    private StackPane drag_area;
    @FXML
    Label QNum;
    @FXML
    private CustomTextField opt1, opt2, opt3, opt4;

    @FXML
    private ToggleGroup q_options;

    @FXML
    private Label file_name;
    @FXML
    private CustomTextField question_text;
    String file;
    @FXML
    private ToggleGroup t_options;
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
        question_text.setText(questionData.question_txt());
        opt1.setRight(new FontIcon("mdi2a-arrow-expand"));
        opt2.setRight(new FontIcon("mdi2a-arrow-expand"));
        opt3.setRight(new FontIcon("mdi2a-arrow-expand"));
        opt4.setRight(new FontIcon("mdi2a-arrow-expand"));
        opt1.getRight().setCursor(Cursor.DEFAULT);
        opt2.getRight().setCursor(Cursor.DEFAULT);
        opt3.getRight().setCursor(Cursor.DEFAULT);
        opt4.getRight().setCursor(Cursor.DEFAULT);
        opt1.setText(questionData.option_data().options().getFirst());
        opt2.setText(questionData.option_data().options(). get(1));
        opt3.setText(questionData.option_data().options().get(2));
        opt4.setText(questionData.option_data().options().get(3));
        expand(opt1, opt2);
        expand(opt3, opt4);
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
