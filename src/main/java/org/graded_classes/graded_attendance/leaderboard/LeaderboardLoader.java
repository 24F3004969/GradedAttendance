package org.graded_classes.graded_attendance.leaderboard;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.controller.MainController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public class LeaderboardLoader {


    public ArrayList<String> preview = new ArrayList<>();
    public LinkedHashMap<String, AnimationDuration> defaultAnimationDuration = new LinkedHashMap<>();
    public MainController mainController;
    public DurationReaderData duration;

    public LeaderboardLoader(MainController mainController) {
        this.mainController = mainController;
    }

    public Scene load(StackPane leader1, StackPane leader2) {
        var list = brandList("Branding");
        ArrayList<StackPane> panes = new ArrayList<>();
        panes.add(leader1);
        panes.add(leader2);
       preview.add("Leaderboard1");
        preview.add("Leaderboard2");
        for (var a : list) {
            panes.add(new ImageSliderShow(a).getSliderPane());
            preview.add(a.substring(a.lastIndexOf('\\') + 1, a.lastIndexOf('.')));
        }
        duration = new DurationReaderData(mainController, this);
        duration.init();
        System.out.println("File things are done");
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        LayoutAnimator layoutAnimator = new LayoutAnimator(root, this, panes.toArray(new StackPane[0]));
        layoutAnimator.animate();
        return scene;
    }
    public ArrayList<String> brandList(String path) {
        ArrayList<String> brandList = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get("G:/My Drive/" + path))) {
            paths.filter(Files::isRegularFile).forEach(p -> {
                brandList.add(p.toAbsolutePath().toString());
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return brandList;
    }

    public void generateDefaultAnimationDuration() {
        defaultAnimationDuration.put(preview.getFirst(), new AnimationDuration(Duration.seconds(18).toSeconds(), Duration.seconds(2).toSeconds()));
        defaultAnimationDuration.put(preview.get(1), new AnimationDuration(Duration.seconds(18).toSeconds(), Duration.seconds(2).toSeconds()));
        for (int i = 2; i < preview.size(); i++) {
            defaultAnimationDuration.put(preview.get(i), new AnimationDuration(Duration.seconds(7).toSeconds(), Duration.seconds(2).toSeconds()));
        }
    }

    public record WinnerInfo(String name, String garde, String img_path) {
    }

}
