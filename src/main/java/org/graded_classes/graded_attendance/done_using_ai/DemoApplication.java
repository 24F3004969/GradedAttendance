package org.graded_classes.graded_attendance.done_using_ai;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;


public class DemoApplication extends Application {

    static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        File lottieFile = new File("motivation.json");

        Animation animation = LottieFileLoader.load(lottieFile);
        var lb=new LottiePlayer(animation);
        var scene = new Scene(new StackPane(lb),
                animation.width() != null ? animation.width() : 500,
                animation.height() != null ? animation.height() : 500
        );
        stage.setTitle(lottieFile.getName());
        stage.setScene(scene);
        stage.show();
        lb.play();

    }
}