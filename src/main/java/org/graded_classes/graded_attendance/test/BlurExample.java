package org.graded_classes.graded_attendance.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class BlurExample extends Application {

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();

        // Add something visible behind the text (so blur is obvious)
        Rectangle bg = new Rectangle(600, 600, Color.LIGHTSKYBLUE);

        Text text = new Text("Blurred Content");
        text.setFont(Font.font(36));

        root.getChildren().addAll(bg, text);

        // Blur the full StackPane (all children)
        root.setEffect(new GaussianBlur(10));

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Full StackPane Blur");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}