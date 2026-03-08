package org.graded_classes.graded_attendance;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.graded_classes.graded_attendance.controller.HomeController;
import org.graded_classes.graded_attendance.controller.StudentAttendance;

import java.io.IOException;

import static org.graded_classes.graded_attendance.GradedResourceLoader.loadURL;

public class GradedFxmlLoader {


    public  Node createView(R path) {
        Node parent;
        try {
            parent = new FXMLLoader(loadURL(path.getPath())).load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parent;
    }

    public Node createView(R path, Object controller) {
        Node parent;
        try {
            var v = new FXMLLoader(loadURL(path.getPath()));
            v.setControllerFactory(c -> controller);
            parent = v.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parent;
    }

}