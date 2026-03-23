package org.graded_classes.graded_attendance.leaderboard;


import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.GradedResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    public static ArrayList<String> preview = new ArrayList<>();
    public static LinkedHashMap<String, AnimationDuration> defaultAnimationDuration = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader1 = new FXMLLoader(GradedResourceLoader.loadURL("fxml/leaderboard/leader_board_view1.fxml"));
        FXMLLoader loader2 = new FXMLLoader(GradedResourceLoader.loadURL("fxml/leaderboard/leader_board_view2.fxml"));
        StudentDataLoader studentDataLoader = new StudentDataLoader();
        var l1 = new Leaderboard1(studentDataLoader);
        var l2 = new LeaderBoard2(studentDataLoader);
        loader1.setControllerFactory(_ -> l1);
        loader2.setControllerFactory(_ -> l2);
        StackPane leader1 = loader1.load();
        StackPane leader2 = loader2.load();
        var treeMap = listOfWinners("Winners");
        var list = brandList("Branding");
        ArrayList<StackPane> panes = new ArrayList<>();
        panes.add(leader1);
        panes.add(leader2);
        preview.add("Leaderboard1");
        preview.add("Leaderboard2");
        for (var m : treeMap.keySet()) {
            panes.add(new ImageSliderShow(treeMap.get(m).name,
                    treeMap.get(m).garde, treeMap.get(m).img_path).
                    getSliderPane());
        }
        for (var a : list) {
            panes.add(new ImageSliderShow(a).getSliderPane());
            preview.add(a.substring(a.lastIndexOf('\\') + 1, a.lastIndexOf('.')));
        }
        DurationReaderData.init();
        System.out.println("File things are done");
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        LayoutAnimator layoutAnimator = new LayoutAnimator(root, panes.toArray(new StackPane[0]));
        layoutAnimator.animate();
        stage.setScene(scene);
        stage.setTitle("Graded Management");
       /* stage.getIcons().add(new Image(Objects.requireNonNull(getClass().
                getResourceAsStream("icons/__logo.png"))));*/
        Stage stage2 = new Stage();
        stage2.setTitle("Points Table");
       /* stage2.getIcons().add(new Image(Objects.requireNonNull(getClass().
                getResourceAsStream("icons/__logo.png"))));*/
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        var pointsTable = new FXMLLoader(LeaderboardResourcesLoader.loadURL("fxml/data_edit.fxml"));
        pointsTable.setControllerFactory(_ -> new PointsTable(studentDataLoader,
                l1.customViews,
                l2.customViews,
                stage2));
        stage2.setScene(new Scene(pointsTable.load(), 700, 600));
        stage.show();
        scene.setOnKeyPressed(event -> {
            stage.setFullScreen(event.getCode() == KeyCode.F11 && !stage.isFullScreen());
            if (event.getCode() == KeyCode.P) {
                stage2.show();
            }
        });
    }

    public TreeMap<Integer, WinnerInfo> listOfWinners(String path) {
        TreeMap<Integer, WinnerInfo> brandList = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(Paths.get("G:/My Drive/" + path))) {
            paths.filter(Files::isRegularFile).forEach(p -> {
                String filename = p.getFileName().toString();
                int id = Integer.parseInt(filename.substring(0, filename.indexOf('-')));
                String _class = filename.substring(filename.indexOf('-') + 1, filename.lastIndexOf('-'));
                String name = filename.substring(filename.lastIndexOf('-') + 1, filename.lastIndexOf('.'));
                brandList.put(id, new WinnerInfo(name, _class, p.toAbsolutePath().toString()));
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return brandList;
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

    public static void generateDefaultAnimationDuration() {
        defaultAnimationDuration.put(preview.getFirst(), new AnimationDuration(Duration.seconds(18).toSeconds(), Duration.seconds(2).toSeconds()));
        defaultAnimationDuration.put(preview.get(1), new AnimationDuration(Duration.seconds(18).toSeconds(), Duration.seconds(2).toSeconds()));
        for (int i = 2; i < preview.size(); i++) {
            defaultAnimationDuration.put(preview.get(i), new AnimationDuration(Duration.seconds(7).toSeconds(), Duration.seconds(2).toSeconds()));
        }
    }

    public record WinnerInfo(String name, String garde, String img_path) {
    }

}
