package org.graded_classes.graded_attendance.new_features_and_ai_slop;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.GradedResourceLoader;

public class CameraApp extends Application {
    CameraTest cameraTest;

    @Override
    public void start(Stage stage) throws Exception {
        OpenCVLoader.loadOpenCV();
        FXMLLoader loader =
                new FXMLLoader(GradedResourceLoader.loadURL("fxml/camera.fxml"));
        cameraTest = new CameraTest();
        loader.setControllerFactory(_ -> cameraTest);
        Scene scene = new Scene(loader.load());
        stage.setTitle("OpenCV Camera Test");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        cameraTest.stopCamera();
    }

}
