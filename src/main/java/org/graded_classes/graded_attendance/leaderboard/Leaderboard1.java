package org.graded_classes.graded_attendance.leaderboard;


import atlantafx.base.util.Animations;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Leaderboard1 implements Initializable {
    public Text title;
    @FXML
    private VBox board;
    @FXML
    private ImageView level1Img, level3Img, level2Img;
    @FXML
    private Label level2Name, level2Title, level1Title, level1Name, level3Name;
    @FXML
    private Label level1Point, level2Point, level3Point;
    StudentDataLoader studentDataLoader;
    public ArrayList<CustomView> customViews = new ArrayList<>();
    MainController mainController;

    public Leaderboard1(StudentDataLoader studentDataLoader, MainController mainController) {
        this.studentDataLoader = studentDataLoader;
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startLoop();
    }

    private void startLoop() {

        Timeline cycle = new Timeline(
                new KeyFrame(Duration.ZERO, e -> addItems()),
                new KeyFrame(Duration.seconds(5), e -> removeItems())
        );

        cycle.setCycleCount(Animation.INDEFINITE);
        cycle.play();
    }

    private void addItems() {

        for (int i = 0; i <=4; i++) {

            int index = i;

            Timeline delay = new Timeline(
                    new KeyFrame(Duration.millis(150 * index), ev -> {

                        var msg = mainController.gradedFxmlLoader.createView(R.leader_view);

                        board.getChildren().add(msg);
                        board.setFillWidth(true);
                        board.setMinHeight(300);
                        Platform.runLater(() -> {
                            Animations.slideInLeft(msg, Duration.millis(250)).playFromStart();
                        });

                    })
            );

            delay.play();
        }
    }

    private void removeItems() {

        for (Node node : board.getChildren()) {

            Platform.runLater(() -> {
                var out = Animations.slideOutRight(node, Duration.millis(250));

                out.setOnFinished(e -> board.getChildren().remove(node));

                out.playFromStart();
            });
        }
    }
}
