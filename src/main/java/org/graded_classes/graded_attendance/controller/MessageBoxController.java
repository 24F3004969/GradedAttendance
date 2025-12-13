package org.graded_classes.graded_attendance.controller;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.net.URL;
import java.util.ResourceBundle;

public class MessageBoxController implements Initializable {
    @FXML
    private Text ed;

    @FXML
    private Label last_seen;

    @FXML
    private CustomTextField my_message;

    @FXML
    private Label name;

    @FXML
    private Button send;
    String s_ed;
    String s_last_seen;
    String s_name;

    public MessageBoxController(String s_ed, String s_last_seen, String s_name) {
        this.s_ed = s_ed;
        this.s_last_seen = s_last_seen;
        this.s_name = s_name;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        FontIcon node = new FontIcon();
        send.setGraphic(node);
        node.setStyle("""
                -fx-icon-code: mdmz-send;
                -fx-icon-size: 24;
                """);
        send.getStyleClass().addAll(Styles.ACCENT, Styles.BUTTON_CIRCLE);
        ed.setText(s_ed);
        last_seen.setText(s_last_seen);
        name.setText(s_name);
    }
}
