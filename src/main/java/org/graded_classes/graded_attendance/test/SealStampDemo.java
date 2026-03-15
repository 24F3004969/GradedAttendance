package org.graded_classes.graded_attendance.test;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.GradedResourceLoader;

import java.io.File;

public class SealStampDemo extends Application {

    // Source - https://stackoverflow.com/a/38409590
// Posted by fabian, modified by community. See post 'Timeline' for change history
// Retrieved 2026-03-15, License - CC BY-SA 3.0

    @Override
    public void start(Stage primaryStage) {
        //Create the pane
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);

        Group textGroup = new Group();

        //Font class instance
        Font font = Font.font("Arial", FontWeight.BOLD, FontPosture.REGULAR, 12);
        //Welcome to Java string
        String welcome = " ★ MANGO 2026★ ★ GRADED COACHING CLASSES ★";
        double rotation = 90;

        double radius = 50d;

        //Loop
        for (char c : welcome.toCharArray()) {
            // ignore whitespace, otherwise add rotated char
            if (!Character.isWhitespace(c)) {
                Text text = new Text(Character.toString(c));
                text.setFont(font);
                text.setFill(Color.web("#1C75BCFF"));
                Rotate rotationMatrix = new Rotate(rotation, 0, radius);
                text.getTransforms().add(rotationMatrix);

                textGroup.getChildren().add(text);
            }
            rotation += 8.5;
        }
        pane.getChildren().add(textGroup);
        var imageView = new ImageView(new Image(GradedResourceLoader.load("icons/ed_short.png")));
        imageView.setFitWidth(70);
        imageView.setPreserveRatio(true);
        pane.getChildren().add(imageView);
        //Create the scene for the application
        Scene scene = new Scene(pane, 500, 500);

        primaryStage.setTitle("Characters around circle");
        primaryStage.setScene(scene);
        try {
            SnapshotUtil.exportFxmlNodeAsPngOffscreen(pane,new File("stamp.png"),7);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //Display
        primaryStage.show();
    }

}
