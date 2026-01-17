package org.graded_classes.graded_attendance.components;


import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.io.Closeable;

public class SizingSandbox extends Group  implements Closeable {

    public SizingSandbox(Node... nodes) {
        new Scene(this);
        getChildren().addAll(nodes);
        layItOut();
    }

    @Override
    public void close() {
        try {
            getChildren().removeAll();
        } catch (Exception e) { }
    }

    private void layItOut() {
        applyCss();
        layout();
    }
}
