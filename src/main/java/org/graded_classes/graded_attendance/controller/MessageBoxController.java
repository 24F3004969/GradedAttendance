package org.graded_classes.graded_attendance.controller;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.graded_classes.graded_attendance.data.MessageSender;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

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
    String telegram_id;

    public MessageBoxController(String s_ed, String s_last_seen, String s_name, String telegram_id, MessageSender sender) {
        this.s_ed = s_ed;
        this.s_last_seen = s_last_seen;
        this.s_name = s_name;
        this.telegram_id = telegram_id;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        FontIcon node = new FontIcon();
        send.setGraphic(node);
        node.setStyle("""
                -fx-icon-code: mdmz-send;
                -fx-icon-size: 24;
                """);
        HBox add_list = new HBox();
        Button add = new Button();
        Button emoji = new Button();
        add.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.ACCENT, Styles.FLAT);
        add.setGraphic(new FontIcon(Material2AL.ADD));
        emoji.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.ACCENT, Styles.FLAT);
        emoji.setGraphic(new FontIcon(Material2AL.
                EMOJI_EMOTIONS));
        add_list.getChildren().addAll(add, emoji);
        my_message.setLeft(add_list);
        add_list.setAlignment(Pos.CENTER_LEFT);
        send.getStyleClass().addAll(Styles.ACCENT, Styles.BUTTON_CIRCLE);
        ed.setText(s_ed);
        last_seen.setText(s_last_seen);
        name.setText(s_name);
    }

    @FXML
    void sendMessage() {
        if (my_message.getText() != null && !my_message.getText().isEmpty() && telegram_id != null && !telegram_id.isEmpty()) {

        }
    }
}
