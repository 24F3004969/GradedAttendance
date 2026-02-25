package org.graded_classes.graded_attendance.controller.quiz;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.TilePane;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.components.LatexView;
import org.graded_classes.graded_attendance.controller.MainController;

import java.util.ArrayList;

public class LatexEditor {
    MainController mainController;
    ArrayList<String> text = new ArrayList<>();

    public LatexEditor(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private TextArea latexCode;

    @FXML
    private TextArea latexText;

    @FXML
    private LatexView latexView;

    @FXML
    private TextArea plainText;

    @FXML
    private TilePane tilePane;

    @FXML
    void addLatex() {
        if (!latexText.getText().isEmpty()) {
            text.add(latexText.getText());
            addToViewOrder(latexText.getText());
            latexText.clear();
            renderLatex();
        }
    }

    @FXML
    void addNewLine() {

    }

    @FXML
    void addPlainText() {
        if (!plainText.getText().isEmpty()) {
            var ab = plainText.getText().split("\n");
            StringBuilder x = new StringBuilder();
            for (var st : ab) {
                x.append("\\text{%s}\\\\".formatted(st));
            }
            text.add(x.toString());
            addToViewOrder(plainText.getText());
            plainText.clear();
            renderLatex();
        }
    }

    private void addToViewOrder(String prompt) {
        Button button = (Button)
                mainController.
                        gradedFxmlLoader.
                        createView(R.triangular_button);
        button.setText(prompt);
        tilePane.getChildren().add(button);
        Button cross= (Button) button.getGraphic();
        cross.setOnMouseClicked(event -> {
            tilePane.getChildren().remove(button);
        });

    }

    private void renderLatex() {
        StringBuilder finalText = new StringBuilder();
        for (String line : text) {
            finalText.append(line);
        }
        System.out.println(finalText);
        latexCode.setText(finalText.toString());
        latexView.setFormula(finalText.toString());
    }

}
