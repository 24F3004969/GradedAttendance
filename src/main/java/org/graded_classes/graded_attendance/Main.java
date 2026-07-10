package org.graded_classes.graded_attendance;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.controller.home.MainController;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

public class Main extends Application {
    public static AppMode appMode = AppMode.DEV;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GradedResourceLoader.loadURL("fxml/main_layout.fxml"));
        fxmlLoader.setControllerFactory(_ -> new MainController(stage));
        Parent root = fxmlLoader.load();
        var scene = new Scene(root);
        long millis = 1773253800000L;
        Instant instant = Instant.ofEpochMilli(millis);
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate date = instant.atZone(zone).toLocalDate();
        stage.setTitle("Graded Management");
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.setScene(scene);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/app_icon.png"))));
        stage.setOnCloseRequest(_ -> System.exit(1));
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            appMode = AppMode.PRODUCTION;
        } else if (Objects.equals(args[0], "dev")) {
            appMode = AppMode.DEV;
        }
        Application.launch(Main.class);
    }
}




