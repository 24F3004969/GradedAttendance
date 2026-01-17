package org.graded_classes.graded_attendance.controller;

import atlantafx.base.theme.Styles;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatBubbleSendController implements Initializable {
    @FXML
    private Label message1;

    @FXML
    private Label roll_num;

    @FXML
    private FontIcon status;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        status.setStyle("""
                -fx-icon-code: mdoal-done;
                /*-fx-icon-color:#0969da;*/
                """);

    }
}
