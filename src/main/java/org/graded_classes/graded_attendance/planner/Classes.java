package org.graded_classes.graded_attendance.planner;

import atlantafx.base.controls.ModalPane;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.graded_classes.graded_attendance.R;

public class Classes {
    Planner planner;
    public Classes(Planner planner) {
        this.planner = planner;
    }

    @FXML
    public void onClassSelected(MouseEvent mouseEvent) {
        Label label = (Label) ((StackPane) mouseEvent.getSource()).lookup("#class-name");
        planner.breadCrumb.getSelectedCrumb().setValue(planner.breadCrumb.getSelectedCrumb().getValue()+" ("+label.getText()+")");
        planner.breadCrumb.setSelectedCrumb(planner.getTreeItemByIndex(planner.root, 1));
        planner.subjectView = planner.createView(R.subject, new Subject(planner,label.getText()));
        planner.div.setContent(planner.subjectView);
    }
}
