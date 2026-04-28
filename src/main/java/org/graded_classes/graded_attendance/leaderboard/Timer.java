package org.graded_classes.graded_attendance.leaderboard;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.graded_classes.graded_attendance.leaderboard.LeaderboardLoader.defaultAnimationDuration;
import static org.graded_classes.graded_attendance.leaderboard.LeaderboardLoader.preview;


public class Timer implements Initializable {
    @FXML
    private ListView<HBox> listView;
    ArrayList<HBox> list = new ArrayList<>();
    Stage stage;

    public Timer(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void onApply() {
        DurationReaderData.updateDurationInDatabase();
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText("Animation timing got updated");
        alert.show();
        stage.close();
    }

    @FXML
    void onClose() {
        stage.close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        for (String t : preview) {
            /*  double value = (i == 0) ? defaultAnimationDuration.get(t).getLayoutDuration() :
                    (defaultAnimationDuration.get(preview.get(i)).getLayoutDuration() - defaultAnimationDuration.get(preview.get(i - 1)).getLayoutDuration());*/
            var layout = new FXMLLoader(LeaderboardResourcesLoader.loadURL("fxml/layout_animator_timer.fxml"));

            layout.setControllerFactory(_ -> new LayoutAnimatorTimer(t, defaultAnimationDuration.get(t).layoutDuration, defaultAnimationDuration.get(t).getFadeTime()));
            try {
                list.add(layout.load());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        listView.setItems(FXCollections.observableList(list));
    }

    @FXML
    private void about() {
        Stage timerStage = new Stage();
        timerStage.setTitle("About");
        try {
            var layout = new FXMLLoader(LeaderboardResourcesLoader.loadURL("fxml/about.fxml"));
            timerStage.setScene(new Scene(layout.load()));
            timerStage.getIcons().add(new Image(Objects.requireNonNull(getClass().
                    getResourceAsStream("icons/__logo.png"))));
            timerStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
