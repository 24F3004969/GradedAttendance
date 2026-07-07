package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.layout.TilePane;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.components.LatexView;
import org.graded_classes.graded_attendance.controller.MainController;


import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import static org.graded_classes.graded_attendance.GradedResourceLoader.loadURL;

public class LatexEditor implements Initializable {
    MainController mainController;
    ArrayList<String> text = new ArrayList<>();
    int selectedButton = -1;
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
    Button selectAppButton;

    @FXML
    void addLatex() {
        if (!latexText.getText().isEmpty()) {
            if (selectedButton == -1) {
                text.add(latexText.getText());
                addToViewOrder(latexText.getText(), "latex");
            } else {
                text.set(selectedButton, latexText.getText());
                ((Button) (tilePane.getChildren().get(selectedButton))).setText(latexText.getText());
                selectedButton = -1;
            }
            latexText.clear();
            renderLatex();
        }
    }

    @FXML
    void addNewLine() {
        text.add("\\\\");
        addToViewOrder("\\n", "newLine");
        renderLatex();
    }

    @FXML
    void addPlainText() {

        if (!plainText.getText().isEmpty()) {
            var ab = plainText.getText().split("\n");
            StringBuilder x = new StringBuilder();
            for (var st : ab) {
                x.append("\\text{%s}".formatted(st));
            }
            if (selectedButton == -1) {
                text.add(x.toString());
                addToViewOrder(plainText.getText(), "plain");
            } else {
                text.set(selectedButton, x.toString());
                ((Button) (tilePane.getChildren().get(selectedButton))).setText(plainText.getText());
                selectedButton = -1;
            }
            plainText.clear();
            renderLatex();
        }
        System.out.println(text);
    }

    private void addToViewOrder(String prompt, String id) {
        Button button = (Button)
                mainController.
                        gradedFxmlLoader.
                        createView(R.triangular_button);
        button.setText(prompt);
        tilePane.getChildren().add(button);
        button.setId(id + "," + (text.size() - 1));
        button.setOnMouseClicked(e -> {
            if (selectAppButton != null && selectAppButton == button) {
                selectAppButton.getStylesheets().removeLast();
                selectAppButton = null;

            } else if (selectAppButton != null) {
                selectAppButton.getStylesheets().removeLast();
                button.getStylesheets().add(loadURL("css/selectButton.css").toExternalForm());
                selectAppButton = button;
            } else {
                button.getStylesheets().add(loadURL("css/selectButton.css").toExternalForm());
                selectAppButton = button;

            }
            if (button.getId().contains("plain")) {
                plainText.setText(button.getText());
                selectedButton = Integer.parseInt(button.getId().split(",")[1]);
            } else if (button.getId().contains("latex")) {
                latexText.setText(button.getText());
                selectedButton = Integer.parseInt(button.getId().split(",")[1]);

            }

        });
        Button cross = (Button) button.getGraphic();
        cross.setOnMouseClicked(event -> {
            tilePane.getChildren().remove(button);
            text.remove(Integer.parseInt(button.getId().split(",")[1]));
            selectedButton = -1;
            renderLatex();
        });

    }

    private void renderLatex() {
        StringBuilder finalText = new StringBuilder();
        for (String line : text) {
            if (line.equals("\\\\"))
                finalText.append(line).append("\n");
            else
                finalText.append(line);
        }
        latexCode.setText(finalText.toString());
        latexView.setFormula(finalText.toString());
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
