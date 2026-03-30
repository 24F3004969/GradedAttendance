package org.graded_classes.graded_attendance.leaderboard;


import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

public class Leaderboard1 implements Initializable {
    public Text title;
    @FXML
    VBox root_layout;
    @FXML
    ListView<CustomView> customListView;
    StudentDataLoader studentDataLoader;
    public ArrayList<CustomView> customViews = new ArrayList<>();

    public Leaderboard1(StudentDataLoader studentDataLoader) {
        this.studentDataLoader = studentDataLoader;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.execute(() -> {
                var vr = studentDataLoader.getSortedStudentList();
                firstThree(vr);
                for (int i = 3; i <15; i++) {
                    var k = vr.get(i);
                    customViews.add(new CustomView(i+1,
                            Name.make_word_name(k.name().trim()), "Class " + k.grade(),
                            "" + (int) k.points(), "#68926d20"));
                }
                var list = FXCollections.observableList(customViews);
                customListView.setItems(list);
            });
        }

    }

    private void firstThree(List<StudentScore> vr) {
        var k1 = vr.get(0);
        var k2 = vr.get(1);
        var k3 = vr.get(2);
        customViews.add(new CustomView(1, "icons/leaderboard/b2.png",
                Name.make_word_name(k1.name()), "Class " + k1.grade(),
                "" + (int) k1.points(), "#C29B0C20"));
        customViews.add(new CustomView(2,
                "icons/leaderboard/b3.png", Name.make_word_name(k2.name()),
                "Class " + k2.grade(),
                "" + (int) k2.points(), "#98afcf20"));
        customViews.add(new CustomView(3,
                "icons/leaderboard/b4.png", Name.make_word_name(k3.name()),
                "Class " + k3.grade(),
                "" + (int) k3.points(), "#99422720"));

    }

}
