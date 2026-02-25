package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class QuizGenerator implements Initializable {
    @FXML
    public TreeView<String> quizTree;
    MainController mainController;
    @FXML
    public BorderPane quiz_gen_layout;
    TreeItem<String> rootItem = new TreeItem<>("New Topic");

    public QuizGenerator(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void onNewTopic() {
        var newTopic = mainController.gradedFxmlLoader.createView(R.newTopic, new QuizTopic(rootItem,mainController.modalPane));
        mainController.modalPane.show(newTopic);
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TreeItem<String> webItem = new TreeItem<>("Integer");
        webItem.setGraphic(new FontIcon("mdi2f-folder"));

        TreeItem<String> javaItem = new TreeItem<>("Fraction");
        javaItem.setGraphic(new FontIcon("mdi2f-folder"));
        rootItem.getChildren().add(webItem);
        rootItem.getChildren().add(javaItem);
        quizTree.setRoot(rootItem);
        quizTree.setShowRoot(true);
        quizTree.setCellFactory(_ -> getTreeCell());
        quizTree.setShowRoot(false);
    }

    private TreeCell<String> getTreeCell() {
        return new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }

                setText(item);

                if (getTreeItem() != null) {
                    setGraphic(getTreeItem().getGraphic());
                }

                TreeItem<String> ti = getTreeItem();
                if (rootItem.getChildren().contains(ti))
                    setContextMenu(createMenu(ti, ti.getValue()));
            }
        };
    }

    private ContextMenu createMenu(TreeItem<String> target, String name) {
        MenuItem addQuiz = getMenuItem(target, name);

        MenuItem rename = new MenuItem("Rename");
        rename.setGraphic(new FontIcon("mdi2r-rename"));
        rename.setOnAction(e -> {
            target.setValue(name + " (Renamed)");
        });

        MenuItem delete = new MenuItem("Delete");
        delete.setGraphic(new FontIcon("mdi2d-delete"));
        delete.setOnAction(e -> {
            if (target.getParent() != null) target.getParent().getChildren().remove(target);
        });

        return new ContextMenu(addQuiz, rename, new SeparatorMenuItem(), delete);
    }

    private MenuItem getMenuItem(TreeItem<String> target, String name) {
        MenuItem addQuiz = new MenuItem("Add " + name + " Quiz");
        addQuiz.setGraphic(new FontIcon("mdi2n-note"));
        addQuiz.setOnAction(e -> {
            var newQuiz = mainController.gradedFxmlLoader.createView(R.newQuiz, new NewQuiz(target, this, mainController));
            mainController.modalPane.show(newQuiz);
        });
        return addQuiz;
    }
}
