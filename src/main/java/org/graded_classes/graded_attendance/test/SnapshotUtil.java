package org.graded_classes.graded_attendance.test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.SnapshotParameters;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.GradedResourceLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SnapshotUtil {

    public static void exportFxmlNodeAsPngOffscreen(Parent node, File file, double scale) throws Exception {
        StackPane container = new StackPane(node);
        new Scene(container);
        container.applyCss();
        container.layout();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(Transform.scale(scale, scale));

        var lb = container.getBoundsInParent();
        int w = (int) Math.ceil(lb.getWidth() * scale);
        int h = (int) Math.ceil(lb.getHeight() * scale);

        WritableImage img = new WritableImage(w, h);
        WritableImage result = container.snapshot(params, img);

        saveAsPng(result, file);
    }

    public static void saveAsPng(WritableImage fxImage, File file) throws IOException {
        BufferedImage buffered = SwingFXUtils.fromFXImage(fxImage, null);
        ImageIO.write(buffered, "png", file);
    }

}
