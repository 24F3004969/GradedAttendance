package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class QuizGenerator implements Initializable {
    @FXML
    private TreeView<String> quizTree;
    MainController mainController;

    public QuizGenerator(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void onNewTopic() {
        var newTopic = mainController.gradedFxmlLoader.createView(R.newTopic);
        mainController.modalPane.show(newTopic);
    }




    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        TreeItem<String> rootItem = new TreeItem<>("New Topic");
        TreeItem<String> webItem = new TreeItem<>("Integer");
        webItem.setGraphic(new FontIcon("mdi2f-folder"));

        TreeItem<String> javaItem = new TreeItem<>("Fraction");
        javaItem.setGraphic(new FontIcon("mdi2f-folder"));

        for (int i = 1; i <= 10; i++) {
            var item = new TreeItem<>("" + i);
            item.setGraphic(new FontIcon("mdi2n-note"));
            webItem.getChildren().add(item);
        }

        for (int i = 1; i <= 10; i++) {
            var item = new TreeItem<>("" + i);
            item.setGraphic(new FontIcon("mdi2n-note"));
            javaItem.getChildren().add(item);
        }
        rootItem.getChildren().add(webItem);
        rootItem.getChildren().add(javaItem);
        quizTree.setRoot(rootItem);
        quizTree.setShowRoot(true);
        quizTree.setCellFactory(tv -> new TreeCell<>() {
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
                if (ti == webItem) {
                    setContextMenu(createMenu(webItem,"Integer"));
                } else if (ti == javaItem) {
                    setContextMenu(createMenu(javaItem,"Fraction"));
                } else {
                    setContextMenu(null);
                }
            }
        });
        quizTree.setShowRoot(false);
    }

    private ContextMenu createMenu(TreeItem<String> target,String name) {
        MenuItem addQuiz = new MenuItem("Add "+name+" Quiz");
        addQuiz.setGraphic(new FontIcon("mdi2n-note"));
        addQuiz.setOnAction(e -> {
           /* var newChild = new TreeItem<>("New "+name+" Quiz");
            newChild.setGraphic(new FontIcon("mdi2n-note"));
            target.getChildren().add(newChild);
            target.setExpanded(true);*/
            var newQuiz = mainController.gradedFxmlLoader.createView(R.newQuiz);
            mainController.modalPane.show(newQuiz);
        });

        MenuItem rename = new MenuItem("Rename");
        rename.setGraphic(new FontIcon("mdi2r-rename"));
        rename.setOnAction(e -> {
            target.setValue(name+" (Renamed)");
        });

        MenuItem delete = new MenuItem("Delete");
        delete.setGraphic(new FontIcon("mdi2d-delete"));
        delete.setOnAction(e -> {
            if (target.getParent() != null) target.getParent().getChildren().remove(target);
        });

        return new ContextMenu(addQuiz, rename, new SeparatorMenuItem(), delete);
    }
}
