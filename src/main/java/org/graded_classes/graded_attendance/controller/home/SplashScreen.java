package org.graded_classes.graded_attendance.controller.home;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.data.MessageSender;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashScreen implements Initializable {
    @FXML
    ProgressBar progressBar;
    Timeline timeline;
    Stage stage;
    MessageSender messageSender;

    public SplashScreen(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startTimer();
    }

    private void startTimer() {
        timeline = new Timeline(new KeyFrame(Duration.millis(5), e -> {
            progressBar.setProgress(progressBar.getProgress() + 0.0007);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

}
