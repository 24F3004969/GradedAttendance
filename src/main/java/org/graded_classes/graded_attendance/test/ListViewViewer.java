package org.graded_classes.graded_attendance.test;



import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ListViewViewer extends Application {
    private static final int MAX_MESSAGES = 20;
    private static final int DISPLAYED_MESSAGES = 8;
    private static final int CELL_HEIGHT = 24;

    private int msgNum = 0;

    @Override
    public void start(Stage stage) {
        ListView<String> list = new ListView<>();
        ObservableList<String> items = list.getItems();
        for (int i = 0; i < DISPLAYED_MESSAGES; i++)  {
            items.add("");
        }
        list.setMinHeight(ListView.USE_PREF_SIZE);
        list.setPrefHeight(DISPLAYED_MESSAGES * CELL_HEIGHT + 2);
        list.setMaxHeight(ListView.USE_PREF_SIZE);

        stage.setScene(new Scene(list));
        stage.show();

       // simulateMessaging(list);
    }

    private void addMessage(ObservableList<String> items, String message) {
        if (items.getFirst().isEmpty() || items.size() >= MAX_MESSAGES) {
            items.removeFirst();
        }

        items.add(message);
    }

    private void simulateMessaging(ListView<String> list) {
        Timeline messageMaker = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        _ -> {
                            addMessage(list.getItems(), nextMessage());
                            if (list.getItems().size() > DISPLAYED_MESSAGES) {
                                list.scrollTo(list.getItems().size() - 1);
                            }
                        }
                )
        );

        messageMaker.setCycleCount(15);

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(event -> messageMaker.play());
        pause.play();
    }

    private String nextMessage() {
        return "msg " + msgNum++;
    }

    void main(String[] args) {
        launch(args);
    }
}
