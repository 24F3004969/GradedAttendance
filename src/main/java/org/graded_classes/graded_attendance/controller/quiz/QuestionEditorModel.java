package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.ToggleLabel;
import javafx.scene.layout.VBox;

public class QuestionEditorModel {
    public VBox root;
    public ToggleLabel status;
    public Questions question;
    public QuestionEditorModel(VBox root, Questions question, ToggleLabel status) {
        this.root = root;
        this.question = question;
        this.status = status;
    }
}
