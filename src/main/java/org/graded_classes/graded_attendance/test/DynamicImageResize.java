package org.graded_classes.graded_attendance.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.GradedResourceLoader;

public class DynamicImageResize extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Load the image
        Image image = new Image(GradedResourceLoader.load("icons/background_image_for_educational_app_Photo.png")); // Use a valid path
        ImageView imageView = new ImageView(image);

        // Use a layout pane (StackPane is good for centering/filling)
        StackPane root = new StackPane();
        root.getChildren().add(imageView);

        // Create the scene
        Scene scene = new Scene(root, 800, 600); // Initial window size

        // Bind ImageView's fit properties to the Scene's dimensions
        imageView.fitWidthProperty().bind(scene.widthProperty());
        imageView.fitHeightProperty().bind(scene.heightProperty());

        // Optional: Preserve the image's original aspect ratio
        imageView.setPreserveRatio(true);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Dynamic Image Resize");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
