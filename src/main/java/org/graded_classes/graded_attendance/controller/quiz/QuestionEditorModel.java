package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.ToggleLabel;
import javafx.scene.layout.VBox;

public class QuestionEditorModel {
    public VBox root;
    ToggleLabel status;

    public QuestionEditorModel(VBox root, ToggleLabel status) {
        this.root = root;
        this.status = status;
    }
}
