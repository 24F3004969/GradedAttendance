package org.graded_classes.graded_attendance.components;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class ImageCropperAndPreviewApp {

    public ImageView getImageView() {
        return imageView;
    }

    private final ImageView imageView;
    private final Rectangle cropBox;

    private double startX;
    private double startY;
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
        cropBox = new Rectangle();
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
        //setupCropEvents(overlayPane);

    /*    Button openBtn = new Button("Open");
        Button cropBtn = new Button("Crop");
        Button saveBtn = new Button("Save");*/

       /* openBtn.setOnAction(e -> openImage(stage));
        cropBtn.setOnAction(e -> cropImage());
        saveBtn.setOnAction(e -> saveImage(stage));*/

        /*  HBox toolbar = new HBox(10, openBtn, cropBtn, saveBtn);*/
      /*  toolbar.setStyle("-fx-padding:10;");
        root.setTop(toolbar);*/
        root.setCenter(imageContainer);
        // Scene scene = new Scene(root, 1000, 800);
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
    /*    stage.setTitle("JavaFX StackPane Cropper");
        stage.setScene(scene);
        stage.show();*/
    }

    private void openImage(Stage stage) {

        FileChooser chooser = new FileChooser();

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Images",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.bmp"
                )
        );

        File file = chooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        Image image = new Image(file.toURI().toString());
        imageView.setImage(image);

        cropBox.setVisible(false);
    }

    private void setupCropEvents(Pane overlayPane) {

        overlayPane.setOnMousePressed(e -> {

            if (imageView.getImage() == null)
                return;

            startX = e.getX();
            startY = e.getY();

            cropBox.setVisible(true);

            cropBox.setX(startX);
            cropBox.setY(startY);
            cropBox.setWidth(0);
            cropBox.setHeight(0);
        });

        overlayPane.setOnMouseDragged(e -> {

            if (imageView.getImage() == null)
                return;

            double endX = e.getX();
            double endY = e.getY();

            double x = Math.min(startX, endX);
            double y = Math.min(startY, endY);

            double width = Math.abs(endX - startX);
            double height = Math.abs(endY - startY);

            cropBox.setX(x);
            cropBox.setY(y);
            cropBox.setWidth(width);
            cropBox.setHeight(height);
        });
    }

    private void cropImage() {

        if (imageView.getImage() == null || !cropBox.isVisible())
            return;

        Image image = imageView.getImage();

        Bounds imageBounds = imageView.getBoundsInParent();

        double scaleX = image.getWidth() / imageBounds.getWidth();
        double scaleY = image.getHeight() / imageBounds.getHeight();

        int x = (int) ((cropBox.getX() - imageBounds.getMinX()) * scaleX);
        int y = (int) ((cropBox.getY() - imageBounds.getMinY()) * scaleY);

        int width = (int) (cropBox.getWidth() * scaleX);
        int height = (int) (cropBox.getHeight() * scaleY);

        x = Math.max(0, x);
        y = Math.max(0, y);

        width = Math.min(width,
                (int) image.getWidth() - x);

        height = Math.min(height,
                (int) image.getHeight() - y);

        if (width <= 0 || height <= 0)
            return;

        WritableImage cropped = new WritableImage(
                image.getPixelReader(),
                x,
                y,
                width,
                height
        );

        imageView.setImage(cropped);
        cropBox.setVisible(false);
    }

    private void saveImage(Stage stage) {

        if (imageView.getImage() == null)
            return;

        FileChooser chooser = new FileChooser();

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "PNG Files",
                        "*.png"
                )
        );

        File file = chooser.showSaveDialog(stage);

        if (file == null)
            return;

        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getAbsolutePath() + ".png");
        }

        try {

            ImageIO.write(
                    SwingFXUtils.fromFXImage(
                            imageView.getImage(),
                            null
                    ),
                    "png",
                    file
            );

            System.out.println("Saved: " + file.getAbsolutePath());

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}