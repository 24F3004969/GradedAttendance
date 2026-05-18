package org.graded_classes.graded_attendance.test;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.effect.*;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.GradedResourceLoader;

import java.io.File;
import java.io.IOException;

public class CoverVideoNoGaps extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(GradedResourceLoader.loadURL("fxml/new_login.fxml"));
        StackPane root = fxmlLoader.load();
        String url = new File("C:/Users/hilal/Downloads/13721064_1080_1920_60fps.mp4").toURI().toString();

        Media media = new Media(url);
        MediaPlayer player = new MediaPlayer(media);
        player.play();
        player.setCycleCount(MediaPlayer.INDEFINITE);
        MediaView mediaView = (MediaView) root.lookup("#mediaView");
        mediaView.setMediaPlayer(player);
        mediaView.setPreserveRatio(true);

        GaussianBlur blur = new GaussianBlur(10);


        ColorAdjust adjust = new ColorAdjust();

        adjust.setInput(blur);

        // Tint overlay (20% alpha)
        ColorInput tint = new ColorInput();
        tint.setPaint(Color.web("#1C75BC", 0.80));

        // Make the tint area follow the Region's size
        tint.xProperty().set(0);
        tint.yProperty().set(0);
        tint.widthProperty().bind(media.widthProperty());
        tint.heightProperty().bind(media.heightProperty());


        Blend blend = new Blend(BlendMode.SRC_OVER);
        blend.setBottomInput(adjust);
        blend.setTopInput(tint);

       // mediaView.setEffect(blend);
        root.setStyle("-fx-padding: 0; -fx-background-color: black;");

        // Clip to ensure absolutely no edge bleed
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        // Recompute fit size whenever container or media dimensions change
        Runnable applyCover = () -> {
            int vw = media.getWidth();
            int vh = media.getHeight();
            if (vw == 0 || vh == 0) return;

            double cw = Math.max(1, root.getWidth());
            double ch = Math.max(1, root.getHeight());

            double scale = Math.max(cw / vw, ch / vh); // “cover”
            mediaView.setFitWidth(vw * scale);
            mediaView.setFitHeight(vh * scale);
            player.play();
        };

        // Listen for container size + media metadata readiness
        root.widthProperty().addListener((o, a, b) -> applyCover.run());
        root.heightProperty().addListener((o, a, b) -> applyCover.run());
        media.widthProperty().addListener((o, a, b) -> applyCover.run());
        media.heightProperty().addListener((o, a, b) -> applyCover.run());
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        Scene scene = new Scene(root, 900, 540, Color.BLACK);
        stage.setScene(scene);
        stage.setTitle("Cover Video (No Gaps, May Crop)");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}