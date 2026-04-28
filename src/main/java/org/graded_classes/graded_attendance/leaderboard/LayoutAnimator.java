package org.graded_classes.graded_attendance.leaderboard;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import static org.graded_classes.graded_attendance.leaderboard.LeaderboardLoader.defaultAnimationDuration;


public class LayoutAnimator {
    private final Pane[] nodes;
    private int imgIndex = 0;
    private final Pane pane;

    public LayoutAnimator(Pane pane, Pane... nodes) {
        this.nodes = nodes;
        this.pane = pane;
    }


    public void animate() {
        Timeline animation = getTimeline();
        pane.getChildren().add(nodes[imgIndex]);
        animation.play();
    }

    private Timeline getTimeline() {
        EventHandler<ActionEvent> eventHandler = e -> {
            if (imgIndex < nodes.length - 1) {
                pane.getChildren().remove(nodes[imgIndex]);
                imgIndex++;
                pane.getChildren().add(nodes[imgIndex]);
                FadeTransition ft = new FadeTransition(Duration.seconds(2), nodes[imgIndex]);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            } else if (imgIndex == nodes.length - 1) {
                imgIndex = 0;
                pane.getChildren().remove(nodes[nodes.length - 1]);
                pane.getChildren().add(nodes[imgIndex]);
                FadeTransition ft = new FadeTransition(Duration.seconds(2), nodes[imgIndex]);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            }
        };
        return getDefaultTimeline(eventHandler);
    }

    private Timeline getDefaultTimeline(EventHandler<ActionEvent> eventHandler) {
        KeyFrame[] keyFrames = new KeyFrame[nodes.length];
        int index = 0;
        double previous_duration = defaultAnimationDuration.firstEntry().getValue().layoutDuration;
        keyFrames[0] = new KeyFrame(Duration.seconds(previous_duration), eventHandler);

        for (var key : defaultAnimationDuration.keySet()) {
            if (index != 0) {
                keyFrames[index] = new KeyFrame(Duration.seconds(defaultAnimationDuration.get(key).layoutDuration + previous_duration), eventHandler);
                previous_duration = defaultAnimationDuration.get(key).layoutDuration + previous_duration;
            }
            index++;
        }
        Timeline animation = new Timeline(keyFrames);
        animation.setCycleCount(Timeline.INDEFINITE);
        return animation;
    }
}