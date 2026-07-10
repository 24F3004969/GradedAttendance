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
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
                new QuizTopic(rootItem, mainController.modalPane,map.values()));
        mainController.modalPane.show(newTopic);
    }

    TreeMap<Integer, String> map = new TreeMap<>();
    TreeMap<Integer, TreeMap<Integer, QuestionData>> allQuestions = new TreeMap<>();
    TreeMap<String, Integer> invertedMap;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        CompletableFuture.runAsync(() -> {
            allQuestions = extractAllQuestionData();
            map = generateTopicMapping();
            generateTreeMap();
            invertedMap = new TreeMap<>();

            for (Map.Entry<Integer, String> entry : map.entrySet()) {
                invertedMap.put(entry.getValue(), entry.getKey());
            }
        });
    }

    private TreeMap<Integer, String> generateTopicMapping() {
        TreeMap<Integer, String> map = new TreeMap<>();
        String sql = "select * from Topics";
        try {
            var conn = mainController.gradedDataLoader.databaseLoader.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                int topicId = rs.getInt("topic_id");
                String topicName = rs.getString("topic_name");
                map.put(topicId, topicName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    private void generateTreeMap() {
        for (var entry : map.keySet()) {
            TreeItem<String> item = new TreeItem<>(map.get(entry));
            item.setGraphic(new FontIcon("mdi2f-folder"));
            int size = allQuestions.get(entry).size();
            for (int i = 1; i <= size; i++) {
                TreeItem<String> e = new TreeItem<>("Question " + i, new FontIcon("mdi2n-note"));
                item.getChildren().add(e);

            }
            rootItem.getChildren().add(item);
            quizTree.setRoot(rootItem);
            quizTree.setShowRoot(true);
            quizTree.setCellFactory(_ -> getTreeCell());
            quizTree.setShowRoot(false);

        }

    }

    private TreeCell<String> getTreeCell() {
        var cell = new TreeCell<String>() {
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


        cell.setOnMouseClicked(event -> {
            if (!cell.isEmpty() && event.getClickCount() == 2) {
                TabPane tabPane = (TabPane) quiz_gen_layout.lookup("#tabs");
                var tb = mainController.gradedFxmlLoader.createView(R.question_editor,
                        new QuestionEditor(mainController, allQuestions.get(invertedMap.get(cell.getItem()))));
                Tab tab = new Tab(cell.getItem());
                tab.setContent(tb);
                tabPane.getTabs().add(tab);
                tabPane.getSelectionModel().select(tab);
            }
        });

        return cell;
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

    public TreeMap<Integer, TreeMap<Integer, QuestionData>> extractAllQuestionData() {
        TreeMap<Integer, TreeMap<Integer, QuestionData>> mapOfQuestion = new TreeMap<>();
        String sql = """
                select *
                    from QuestionOptions
                             join Questions on Questions.question_id = QuestionOptions.question_id
                """;
        try {
            var conn = mainController.gradedDataLoader.databaseLoader.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            int id=0;
            while (rs.next()) {
                int topicId = rs.getInt("topic_id");
                int questionId = rs.getInt("question_id");
                if (mapOfQuestion.containsKey(topicId)) {
                    if (mapOfQuestion.get(topicId).containsKey(questionId)) {
                        mapOfQuestion.get(topicId).get(questionId).
                                option_data().options().add(rs.getString("option_text"));
                        if (rs.getInt("is_correct")==1)
                            mapOfQuestion.get(topicId).get(questionId).
                                    option_data().setOption_index(mapOfQuestion.get(topicId).
                                            get(questionId).option_data().options().size()-1);
                    } else {
                        id++;
                        mapOfQuestion.get(topicId).put(questionId, new QuestionData("" + questionId,
                                rs.getString("topic_id"),
                                rs.getString("user_id"),
                                rs.getString("date_of_making"),
                                rs.getString("type"),
                                rs.getString("level"),
                                rs.getString("question_txt"),
                                rs.getString("question_img_path"), new OptionData(
                                0, new ArrayList<>(List.of(rs.getString("option_text")))
                        )));
                    }

                } else {
                    var map = new TreeMap<Integer, QuestionData>();
                    map.put(questionId, new QuestionData("" + questionId,
                            rs.getString("topic_id"),
                            rs.getString("user_id"),
                            rs.getString("date_of_making"),
                            rs.getString("type"),
                            rs.getString("level"),
                            rs.getString("question_txt"),
                            rs.getString("question_img_path"), new OptionData(
                            0, new ArrayList<>(List.of(rs.getString("option_text"))))));
                    mapOfQuestion.put(topicId, map);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mapOfQuestion;
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
