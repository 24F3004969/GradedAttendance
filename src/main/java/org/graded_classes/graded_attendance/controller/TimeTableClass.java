package org.graded_classes.graded_attendance.controller;

import atlantafx.base.layout.DeckPane;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import static org.graded_classes.graded_attendance.GradedResourceLoader.loadURL;

public class TimeTableClass implements Initializable {
    private HBox left_button;
    private HBox right_button;
    @FXML
    DeckPane deckPane;
    HomeController homeController;

    public TimeTableClass(HomeController homeController) {
        this.homeController = homeController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        VBox[] dialog = new VBox[7];
        for (int i = 0; i <= 6; i++) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(loadURL("fxml/time_table_view.fxml"));
                var timeTableView = new TimeTableView("" + (i + 4), homeController.gradedDataLoader.databaseLoader.getConnection());
                fxmlLoader.setControllerFactory(c -> timeTableView);
                dialog[i] = fxmlLoader.load();
                DeckPane.setLeftAnchor( dialog[i], 0d);
                DeckPane.setRightAnchor( dialog[i], 0d);
                DeckPane.setTopAnchor( dialog[i], 0d);
                DeckPane.setBottomAnchor( dialog[i], 0d);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        deckPane.getChildren().addAll(dialog);

    }
    Supplier<Node> nextItem = () -> {
        var next = (deckPane.getChildren().indexOf(deckPane.getTopNode()) + 1)
                % deckPane.getChildren().size();
        return deckPane.getChildren().get(next);
    };
    @FXML
    void left(MouseEvent event) {
        deckPane.swipeRight(nextItem.get());

    }

    @FXML
    void right(MouseEvent event) {
        deckPane.swipeLeft(nextItem.get());

    }
}
