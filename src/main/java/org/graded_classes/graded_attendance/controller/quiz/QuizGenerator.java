package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.data.ExamData;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class QuizGenerator implements Initializable {
    @FXML
    public TreeView<String> quizTree;
    @FXML
    public HBox topRightQuizView;
    MainController mainController;
    @FXML
    public BorderPane quiz_gen_layout;
    @FXML
    private BorderPane innerPane;
    TreeItem<String> rootItem = new TreeItem<>("New Topic");
    @FXML
    HBox selectedTab;
    Node previouslySelectedNode;

    public QuizGenerator(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void onNewTopic() {
        var newTopic = mainController.gradedFxmlLoader.createView(R.newTopic,
                new QuizTopic(rootItem, mainController.modalPane));
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
            var newQuiz = mainController.gradedFxmlLoader.createView(R.newQuiz,
                    new NewQuiz(target, this, mainController));
            mainController.modalPane.show(newQuiz);
        });
        return addQuiz;
    }

    @FXML
    void changeView(MouseEvent event) {
        HBox source = (HBox) event.getSource();
        String id = source.getId();
        switch (id) {
            case "dashboard" -> System.out.println();
            case "quizGen" -> {
                topRightQuizView.setVisible(true);
                selectedTab.getStyleClass().clear();
                source.getStyleClass().add("tab_selector");
                selectedTab = source;
                quiz_gen_layout.setCenter(previouslySelectedNode);
            }
            case "exam" -> {
                topRightQuizView.setVisible(false);
                selectedTab.getStyleClass().clear();
                source.getStyleClass().add("tab_selector");
                selectedTab = source;
                previouslySelectedNode = quiz_gen_layout.getCenter();
                quiz_gen_layout.setCenter(mainController.
                        gradedFxmlLoader.createView(R.exam_conductor, new ConductExam(mainController, initExamSchedule())));
            }
        }
    }

    ArrayList<ExamData> initExamSchedule() {

        ArrayList<ExamData> examList = new ArrayList<>();

        String sql = """
                    SELECT e.exam_id,
                           e.class,
                           e.exam_date,
                           e.room_no,
                           e.subject,
                           e.start_time,
                           e.end_time,
                           t.topic_id,
                           t.topic_name
                    FROM ExamScheduler e
                    LEFT JOIN Topics t ON e.topic_id = t.topic_id
                    ORDER BY e.exam_date, e.start_time
                """;
        try {
            var conn = mainController.gradedDataLoader.databaseLoader.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String id = String.valueOf(rs.getInt("exam_id"));
                String classes = rs.getString("class");
                String doe = rs.getString("exam_date");
                String room = rs.getString("room_no");
                String subject = rs.getString("subject");
                String time = rs.getString("start_time") + " - " + rs.getString("end_time");
                String topic_id = rs.getString("topic_id");
                String topic_name = rs.getString("topic_name");
                ExamData exam = new ExamData(
                        id,
                        classes,
                        doe,
                        room,
                        subject,
                        time,
                        topic_id,
                        topic_name
                );
                examList.add(exam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return examList;
    }
}
