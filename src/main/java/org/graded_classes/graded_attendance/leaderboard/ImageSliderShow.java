package org.graded_classes.graded_attendance.leaderboard;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import org.graded_classes.graded_attendance.GradedResourceLoader;

import java.io.IOException;

public class ImageSliderShow {
    final StackPane sliderPane;

    public ImageSliderShow(String name, String grade, String src_path) {
        FXMLLoader loader = new FXMLLoader(LeaderboardResourcesLoader.loadURL("fxml/winners.fxml"));
        loader.setControllerFactory(_ -> new Winners(name, grade, src_path));
        try {
            sliderPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ImageSliderShow(String src_path) {
        FXMLLoader loader = new FXMLLoader(GradedResourceLoader.loadURL("fxml/leaderboard/adds_and_branding.fxml"));
        loader.setControllerFactory(_ -> new AddsAndBranding(src_path));
        try {
            sliderPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public StackPane getSliderPane() {
        return sliderPane;
    }
}
