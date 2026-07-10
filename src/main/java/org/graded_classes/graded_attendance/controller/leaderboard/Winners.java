package org.graded_classes.graded_attendance.controller.leaderboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;


public class Winners implements Initializable {

    @FXML
    private Text winner_class;

    @FXML
    private Text winner_name;

    @FXML
    private ImageView winner_img;
    private String name, grade, src_path;

    public Winners(String name, String grade, String src_path) {
        this.name = name;
        this.grade = grade;
        this.src_path = src_path;
    }

    public Text getWinner_class() {
        return winner_class;
    }

    public void setWinner_class(Text winner_class) {
        this.winner_class = winner_class;
    }

    public Text getWinner_name() {
        return winner_name;
    }

    public void setWinner_name(Text winner_name) {
        this.winner_name = winner_name;
    }

    public ImageView getWinner_img() {
        return winner_img;
    }

    public void setWinner_img(ImageView winner_img) {
        this.winner_img = winner_img;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getSrc_path() {
        return src_path;
    }

    public void setSrc_path(String src_path) {
        this.src_path = src_path;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        winner_img.setImage(new Image(new File(src_path).toURI().toString()));
        winner_name.setText(name);
        winner_class.setText("Class " + grade);
    }
}
