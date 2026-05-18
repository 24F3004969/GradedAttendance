package org.graded_classes.graded_attendance.leaderboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class AddsAndBranding implements Initializable {
    @FXML
    public StackPane root;
    @FXML
    ImageView branding;
    String srcPath;
    public AddsAndBranding(String srcPath) {
        this.srcPath = srcPath;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        branding.setImage(new Image(new File(srcPath).toURI().toString()));
        branding.setPreserveRatio(false);
        branding.setSmooth(true);
        branding.setCache(true);
        root.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null) return;
            branding.fitWidthProperty().bind(scene.widthProperty());
            branding.fitHeightProperty().bind(scene.heightProperty());
        });

    }
}
