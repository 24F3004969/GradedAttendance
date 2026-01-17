package org.graded_classes.graded_attendance.planner;

import atlantafx.base.controls.Breadcrumbs;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.Styles;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import org.graded_classes.graded_attendance.GradedFxmlLoader;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.data.GradedDataLoader;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

public class Planner extends GradedFxmlLoader implements Initializable {
    @FXML
    Breadcrumbs<String> breadCrumb;
    @FXML
    ScrollPane div;
    LinkedHashMap<String, String> subjectMap = new LinkedHashMap<>();
    Breadcrumbs.BreadCrumbItem<String> root;
    Node classView, subjectView;
    String[] items = {"Class", "Subjects", "Math"};
    GradedDataLoader gradedDataLoader;
    ModalPane modalPane;
    public Planner(GradedDataLoader gradedDataLoader, ModalPane modalPane) {
        this.gradedDataLoader = gradedDataLoader;
        this.modalPane = modalPane;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        createClassView();
        root = Breadcrumbs.buildTreeModel(
                items
        );
        breadCrumb.setSelectedCrumb(getTreeItemByIndex(root, 2));
        breadCrumb.setCrumbFactory(crumb -> {
            var btn = new Button(crumb.getValue());
            btn.getStyleClass().add(Styles.FLAT);
            btn.setFocusTraversable(false);
            return btn;

        });
        breadCrumb.setDividerFactory(item -> {
            if (item == null) {
                return new Label("", new FontIcon(MaterialDesignH.HOME));
            }
            return !item.isLast()
                    ? new Label("", new FontIcon(MaterialDesignC.CHEVRON_RIGHT))
                    : null;
        });
    }

    private void createClassView() {
        classView = createView(R.classes, new Classes(this));
        div.setContent(classView);
    }

    <T> Breadcrumbs.BreadCrumbItem<T> getTreeItemByIndex(Breadcrumbs.BreadCrumbItem<T> node, int index) {
        var counter = index;
        var current = node;
        while (counter > 0 && current.getParent() != null) {
            current = (Breadcrumbs.BreadCrumbItem<T>) current.getParent();
            counter--;
        }
        return current;
    }


    public void whenBreadCrumbClicked(Breadcrumbs.BreadCrumbActionEvent<String> stringBreadCrumbActionEvent) {
        if (stringBreadCrumbActionEvent.getSelectedCrumb().getValue().contains("Class")) {
            div.setContent(classView);
            stringBreadCrumbActionEvent.getSelectedCrumb().setValue("Class");
        } else if (stringBreadCrumbActionEvent.getSelectedCrumb().getValue().equals("Subjects")) {
            div.setContent(subjectView);
        }
    }
}
