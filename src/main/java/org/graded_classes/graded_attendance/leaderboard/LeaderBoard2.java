package org.graded_classes.graded_attendance.leaderboard;


import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

public class LeaderBoard2 implements Initializable {
    public Text title;
    @FXML
    VBox root_layout;
    @FXML
    ListView<CustomView> customListView;
    public ArrayList<CustomView> customViews = new ArrayList<>();
    StudentDataLoader studentDataLoader;
    public LeaderBoard2(){}
    public LeaderBoard2(StudentDataLoader studentDataLoader) {
        this.studentDataLoader = studentDataLoader;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.execute(() -> {
                var vr = studentDataLoader.getSortedStudentList();
                for (int i = 16; i <31; i++) {
                    var k = vr.get(i);
                    customViews.add(new CustomView(i,
                            Name.make_word_name(k.name().trim()), "Class " + k.grade(),
                            "" + (int) k.points(), "#68926d20"));
                }

                var list = FXCollections.observableList(customViews);
                customListView.setItems(list);

            });
        }

    }
}

