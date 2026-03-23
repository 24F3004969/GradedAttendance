package org.graded_classes.graded_attendance.leaderboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class AddsAndBranding implements Initializable {
    @FXML
    ImageView branding;
    String srcPath;
    public AddsAndBranding(String srcPath) {
        this.srcPath = srcPath;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
       branding.setImage(new Image(new File(srcPath).toURI().toString()));
    }
}
