package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.graded_classes.graded_attendance.components.LatexView;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class QuestionOption implements Initializable {
    String optionName;
    String latexText;
    String imagePath;
    ToggleGroup toggleGroup;

    public QuestionOption(String optionName, String latexText, String imagePath, ToggleGroup toggleGroup) {
        this.optionName = optionName;
        this.latexText = latexText;
        this.imagePath = imagePath;
        this.toggleGroup = toggleGroup;
    }


    @FXML
    private Label name;

    @FXML
    private ImageView option_image;

    @FXML
    private LatexView option_text;
    @FXML
    private RadioButton radio;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        name.setText(optionName);
        option_text.setFormula(latexText);
       // option_image.setImage(new Image(new File(imagePath).toURI().toString()));
        radio.setToggleGroup(toggleGroup);
    }
}
