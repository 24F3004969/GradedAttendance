package org.graded_classes.graded_attendance;

import atlantafx.base.theme.PrimerLight;
import com.dlsc.fxmlkit.fxml.FxmlKit;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.graded_classes.graded_attendance.controller.MainController;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GradedResourceLoader.loadURL("fxml/splash_screen.fxml"));
       // fxmlLoader.setControllerFactory(_ -> new MainController(stage));
        Parent root = fxmlLoader.load();
        var scene = new Scene(root);
        scene.setFill(Paint.valueOf("#fafafa00"));
        stage.setTitle("Graded Management");
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.setScene(scene);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/app_icon.png"))));
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.centerOnScreen();
        stage.setOnCloseRequest(_ -> System.exit(1));
        stage.show();
    }
}