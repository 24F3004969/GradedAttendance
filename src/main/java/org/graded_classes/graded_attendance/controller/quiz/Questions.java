package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.CustomTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class Questions implements Initializable {
    @FXML
    private StackPane drag_area;

    @FXML
    private CustomTextField opt1, opt2, opt3, opt4;

    @FXML
    private ToggleGroup q_options;

    @FXML
    private CustomTextField question_text;

    @FXML
    private ToggleGroup t_options;
    MainController mainController;
    public  Questions(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void onBasicAction(ActionEvent event) {

    }

    @FXML
    public void handleDragOver(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        if (dragboard.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();

        String file = dragboard.getFiles().toString().substring(1, dragboard.getFiles().toString().length() - 1);
        System.out.println(file);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        question_text.setRight(new FontIcon("mdi2a-arrow-expand"));
        question_text.getRight().setCursor(Cursor.DEFAULT);
        question_text.getRight().setOnMouseClicked(event -> {
            var editor=mainController.gradedFxmlLoader.createView(R.latex_editor);
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
    }

    private void expand(CustomTextField opt1, CustomTextField opt2) {
        opt1.getRight().setOnMouseClicked(event -> {
            var editor=mainController.gradedFxmlLoader.createView(R.latex_editor);
            mainController.modalPane.show(editor);
        });
        opt2.getRight().setOnMouseClicked(event -> {
            var editor=mainController.gradedFxmlLoader.createView(R.latex_editor);
            mainController.modalPane.show(editor);
        });
    }

}
