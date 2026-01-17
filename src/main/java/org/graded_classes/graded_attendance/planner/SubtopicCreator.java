package org.graded_classes.graded_attendance.planner;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.graded_classes.graded_attendance.R;

import java.net.URL;
import java.util.ResourceBundle;

public class SubtopicCreator implements Initializable {
    private final Planner planner;
    private final String name;
    public Label subtopicName;
    public SubtopicCreator(Planner planner, String name) {
        this.planner = planner;
        this.name = name;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
         subtopicName.setText(name);
    }
    @FXML
    void editSubtopic() {
        planner.modalPane.setAlignment(Pos.CENTER);
        planner.modalPane.show(planner.createView(R.edit_sub_topic));
    }

    @FXML
    void removeSubtopic() {

    }
}
