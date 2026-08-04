package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.SVGImageView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import org.graded_classes.graded_attendance.GradedResourceLoader;

import java.net.URL;
import java.util.ResourceBundle;

public class ExamReport implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        icons.setSvgUrl(GradedResourceLoader.load("icons/my-logo.svg"));
    }

    @FXML
    private Text ed;

    @FXML
    private SVGImageView icons;

    @FXML
    private Label id_user;

    @FXML
    private Label name_class;

    @FXML
    private Label recent_message;

    @FXML
    private Label recent_message1;

    @FXML
    private Label today;

}