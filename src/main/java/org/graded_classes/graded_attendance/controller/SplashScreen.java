package org.graded_classes.graded_attendance.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.GradedFxmlLoader;
import org.graded_classes.graded_attendance.data.GradedDataLoader;
import org.graded_classes.graded_attendance.data.MessageSender;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class SplashScreen implements Initializable {
    @FXML
    ProgressBar progressBar;
    Timeline timeline;
    Stage stage;
    //public GradedDataLoader gradedDataLoader = new GradedDataLoader();
    MessageSender messageSender;

    public SplashScreen(Stage stage) {
        this.stage = stage;
        //messageSender = new MessageSender(getToken(),gradedDataLoader);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startTimer();
    }

    private void startTimer() {
        timeline = new Timeline(new KeyFrame(Duration.millis(5), e -> {
            progressBar.setProgress(progressBar.getProgress() + 0.0007);
            /*if (categoryMap != null) {
                stage.close();
                stage = new Stage();
                stage.setTitle("Quiz Time");
                stage.show();
                timeline.stop();
            }*/
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /*public String getToken() {
        String query = "SELECT id FROM token LIMIT 1";

        try (PreparedStatement stmt = gradedDataLoader.databaseLoader.getConnection().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // No token found or error occurred
    }*/

}
