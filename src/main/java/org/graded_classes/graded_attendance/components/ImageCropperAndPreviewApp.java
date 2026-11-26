package org.graded_classes.graded_attendance.components;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class ImageCropperAndPreviewApp {

    public ImageView getImageView() {
        return imageView;
    }

    private final ImageView imageView;
    private double lastX;
    private double lastY;
    private final Button leftButton = new Button(), rightButton = new Button();

    public Button getLeftButton() {
        return leftButton;
    }

    public Button getRightButton() {
        return rightButton;
    }

    public BorderPane getRoot() {
        return root;
    }

    private final BorderPane root = new BorderPane();

    public ImageCropperAndPreviewApp() {
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        Rectangle cropBox = new Rectangle();
        cropBox.setFill(Color.rgb(0, 0, 0, 0.55));
        cropBox.setStroke(Color.WHITE);
        cropBox.setStrokeWidth(2);
        cropBox.setVisible(false);
        Pane overlayPane = new Pane();
        overlayPane.getChildren().add(cropBox);
        StackPane imageContainer = new StackPane();
        leftButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        rightButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        leftButton.setGraphic(new FontIcon(Feather.ARROW_LEFT));
        rightButton.setGraphic(new FontIcon(Feather.ARROW_RIGHT));
        BorderPane.setMargin(leftButton,new Insets(5,5,5,5));
        BorderPane.setMargin(rightButton,new Insets(5,5,5,5));
        root.setStyle("""
                -fx-background-color:#E3F2FD;
                -fx-background-radius: 12;
                """);
        imageContainer.getChildren().addAll(
                imageView,
                overlayPane
        );
        root.setCenter(imageContainer);
        imageView.fitHeightProperty().bind(root.heightProperty());
        imageView.fitWidthProperty().bind(root.widthProperty());

        DoubleProperty zoom =
                new SimpleDoubleProperty(1);

        imageView.scaleXProperty().bind(zoom);
        imageView.scaleYProperty().bind(zoom);

        imageContainer.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                lastX = e.getSceneX();
                lastY = e.getSceneY();
            }
        });
        imageContainer.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {

                double dx = e.getSceneX() - lastX;
                double dy = e.getSceneY() - lastY;

                imageView.setTranslateX(imageView.getTranslateX() + dx);
                imageView.setTranslateY(imageView.getTranslateY() + dy);

                lastX = e.getSceneX();
                lastY = e.getSceneY();
            }
        });
        imageContainer.setOnScroll(e -> {

            if (e.getDeltaY() > 0) {
                zoom.set(zoom.get() * 1.1);
            } else {
                zoom.set(zoom.get() / 1.1);
            }
        });
        root.setLeft(leftButton);
        root.setRight(rightButton);
        BorderPane.setAlignment(leftButton, Pos.CENTER_LEFT);
        BorderPane.setAlignment(rightButton,Pos.CENTER_RIGHT);
    }
}