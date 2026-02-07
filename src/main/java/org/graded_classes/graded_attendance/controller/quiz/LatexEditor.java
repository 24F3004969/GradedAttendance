package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.TilePane;
import org.graded_classes.graded_attendance.components.LatexView;

import java.util.ArrayList;

public class LatexEditor {
    ArrayList<String> text = new ArrayList<>();
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

    }

    @FXML
    void addNewLine() {

    }

    @FXML
    void addPlainText() {
        if (!plainText.getText().isEmpty()) {
            String x="\\text{%s}".formatted(plainText.getText());
            text.add(x);
            renderLatex();
        }
    }

    private void renderLatex() {
        for (String line : text) {
            latexView.setFormula(line);
        }
    }

}
