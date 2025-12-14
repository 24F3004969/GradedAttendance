package org.graded_classes.graded_attendance.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class MessageViewController implements Initializable {

    @FXML
    private Text ed;

    @FXML
    private Label name_class;

    @FXML
    private Label recent_message;
    @FXML
    private Label id_user;
    @FXML
    private Label time;
    private String ed_s, name_s, recent_message_s, time_s, telegram_id;

    public Text getEd() {
        return ed;
    }

    public void setEd(Text ed) {
        this.ed = ed;
    }

    public Label getName_class() {
        return name_class;
    }

    public void setName_class(Label name_class) {
        this.name_class = name_class;
    }

    public Label getRecent_message() {
        return recent_message;
    }

    public void setRecent_message(Label recent_message) {
        this.recent_message = recent_message;
    }

    public Label getTime() {
        return time;
    }

    public void setTime(Label time) {
        this.time = time;
    }

    public String getEd_s() {
        return ed_s;
    }

    public void setEd_s(String ed_s) {
        this.ed_s = ed_s;
    }

    public String getName_s() {
        return name_s;
    }

    public void setName_s(String name_s) {
        this.name_s = name_s;
    }

    public String getRecent_message_s() {
        return recent_message_s;
    }

    public void setRecent_message_s(String recent_message_s) {
        this.recent_message_s = recent_message_s;
    }

    public String getTime_s() {
        return time_s;
    }

    public void setTime_s(String time_s) {
        this.time_s = time_s;
    }

    public MessageViewController(String ed_s, String name_s, String grade_s, String recent_message_s, String time_s, String telegram_id) {
        this.ed_s = ed_s;
        this.name_s = name_s;
        this.recent_message_s = recent_message_s;
        this.time_s = time_s;
        this.telegram_id = telegram_id;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ed.setText(ed_s);
        name_class.setText(name_s);
        recent_message.setText(recent_message_s);
        time.setText(time_s);
        id_user.setText(telegram_id);
    }
}
