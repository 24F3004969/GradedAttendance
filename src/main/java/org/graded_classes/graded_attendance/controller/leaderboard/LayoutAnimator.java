package org.graded_classes.graded_attendance.controller.leaderboard;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.Pane;
import javafx.util.Duration;


public class LayoutAnimator {
    private final Pane[] nodes;
    private int imgIndex = 0;
    private final Pane pane;
    LeaderboardLoader leaderboardLoader;

    public LayoutAnimator(Pane pane, LeaderboardLoader leaderboardLoader,Pane... nodes) {
        this.nodes = nodes;
        this.pane = pane;
        this.leaderboardLoader = leaderboardLoader;
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
        double previous_duration = leaderboardLoader.defaultAnimationDuration.firstEntry().getValue().layoutDuration;
        keyFrames[0] = new KeyFrame(Duration.seconds(previous_duration), eventHandler);
        System.out.println(leaderboardLoader.defaultAnimationDuration);
        for (var key : leaderboardLoader.defaultAnimationDuration.keySet()) {
            if (index != 0) {
                keyFrames[index] = new KeyFrame(Duration.seconds(leaderboardLoader.defaultAnimationDuration.get(key).layoutDuration + previous_duration), eventHandler);
                previous_duration = leaderboardLoader.defaultAnimationDuration.get(key).layoutDuration + previous_duration;
            }
            index++;
        }
        Timeline animation = new Timeline(keyFrames);
        animation.setCycleCount(Timeline.INDEFINITE);
        return animation;
    }
}