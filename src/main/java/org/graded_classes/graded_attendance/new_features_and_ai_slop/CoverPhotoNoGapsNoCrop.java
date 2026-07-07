package org.graded_classes.graded_attendance.new_features_and_ai_slop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;

public class CoverPhotoNoGapsNoCrop extends Application {

    @Override
    public void start(Stage stage) {
        Image image = new Image(new File("G:\\My Drive\\Branding\\info gradeEd.png").toURI().toString());
        ImageView imageView = new ImageView(image);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black; -fx-padding: 0;");
        root.getChildren().add(imageView);

        // Key part: bind to root (not scene) so it matches actual content area
        imageView.fitWidthProperty().bind(root.widthProperty());
        imageView.fitHeightProperty().bind(root.heightProperty());

        // No gaps + no crop => must allow distortion
        imageView.setPreserveRatio(false);

        // Quality tweaks
        imageView.setSmooth(true);
        imageView.setCache(true);

        Scene scene = new Scene(root, 900, 540, Color.BLACK);
        stage.setScene(scene);
        stage.setTitle("Cover Photo (No Gaps, No Crop)");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}