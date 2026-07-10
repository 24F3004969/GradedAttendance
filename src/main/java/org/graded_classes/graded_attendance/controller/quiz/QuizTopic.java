package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.ModalPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.AnchorPane;
import org.graded_classes.graded_attendance.components.FilterComboBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

public class QuizTopic implements Initializable {

    @FXML
    public ComboBox<String> filterBox;
    public FilterComboBox filterComboBox;
    ObservableList<String> observableList = FXCollections.observableArrayList(List.of(

    ));
    TreeItem<String> rootItem;
    ModalPane modalPane;

    public QuizTopic(TreeItem<String> rootItem, ModalPane modalPane, Collection<String> values) {
        this.rootItem = rootItem;
        this.modalPane = modalPane;
        observableList.addAll(values);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        AnchorPane parent = (AnchorPane) filterBox.getParent();
        filterComboBox = new FilterComboBox(observableList);
        parent.getChildren().set(parent.getChildren().indexOf(filterBox), filterComboBox);
        filterComboBox.setPromptText("Subject");
    }

    @FXML
    void addTopic() {
        var item = new TreeItem<>(filterComboBox.getValue());
        item.setGraphic(new FontIcon("mdi2f-folder"));
        rootItem.getChildren().add(item);
        modalPane.hide();
    }
}
