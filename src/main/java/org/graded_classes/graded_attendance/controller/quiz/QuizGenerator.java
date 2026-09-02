package org.graded_classes.graded_attendance.controller.quiz;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.components.BasicParser;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.ExamData;
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.graded_classes.graded_attendance.controller.quiz.QuizTaker.extractResourceToTempFile;

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
    @FXML
    StackPane welcomeScreen;
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
                new QuizTopic(rootItem, mainController.modalPane, map.values()));
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
            Platform.runLater(() ->
            {
                var player = generateLottie();
                welcomeScreen.getChildren().add(player);
                player.play();
                generateTreeMap();
            });
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

    MenuItem saveToDb = new MenuItem("Save to Db");
    MenuItem menuItemDelete = new MenuItem("Delete");
    ContextMenu contextMenu;

    {

        saveToDb.setGraphic(new FontIcon("mdi-database"));
        menuItemDelete.setGraphic(new FontIcon("mdi2d-delete"));
        contextMenu = new ContextMenu(saveToDb, menuItemDelete);
    }

    private void generateTreeMap() {
        for (var entry : map.keySet()) {
            if (allQuestions.containsKey(entry)) {
                TreeItem<String> item = new TreeItem<>(map.get(entry));
                item.setGraphic(new FontIcon("mdi2f-folder"));
                if (allQuestions.containsKey(entry)) {
                    int size = allQuestions.get(entry).size();
                    for (int i = 1; i <= size; i++) {
                        TreeItem<String> e = new TreeItem<>("Question " + i, new FontIcon("mdi2n-note"));
                        item.getChildren().add(e);

                    }
                }
                rootItem.getChildren().add(item);
                quizTree.setRoot(rootItem);
                quizTree.setShowRoot(true);
                quizTree.setCellFactory(_ -> getTreeCell());
                quizTree.setShowRoot(false);

            }
        }

    }

    /* private TreeCell<String> getTreeCell() {
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
                         new QuestionEditor(mainController, allQuestions.get(invertedMap.get(cell.getItem())), "" + invertedMap.get(cell.getItem())));
                 Tab tab = new Tab(cell.getItem());
                 tab.setContent(tb);
                 tabPane.getTabs().add(tab);
                 tabPane.getSelectionModel().select(tab);
             }
         });

         return cell;
     }*/
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

                TreeItem<String> ti = getTreeItem();
                if (ti != null) {
                    setGraphic(ti.getGraphic());

                    // FIX 1: If it's your custom class, extract its own menu directly!
                    if (ti instanceof ContextTreeItem customItem) {
                        setContextMenu(customItem.getContextMenu());
                    }
                    // FIX 2: Fall back to your legacy rule for top-level root items
                    else if (rootItem.getChildren().contains(ti)) {
                        if (getContextMenu() == null) {
                            setContextMenu(createMenu(ti, item));
                        }
                    }
                    // Clear menu for raw sub-items (like "Question 1", "Question 2")
                    else {
                        setContextMenu(null);
                    }
                } else {
                    setGraphic(null);
                    setContextMenu(null);
                }
            }
        };

        // Double-click Tab handling remains exactly the same below...
        cell.setOnMouseClicked(event -> {
            if (!cell.isEmpty() && cell.getItem() != null && event.getClickCount() == 2) {
                TabPane tabPane = (TabPane) quiz_gen_layout.lookup("#tabs");
                var itemKey = invertedMap.get(cell.getItem());
                if (itemKey != null) {
                    var tb = mainController.gradedFxmlLoader.createView(
                            R.question_editor,
                            new QuestionEditor(mainController, allQuestions.get(itemKey), String.valueOf(itemKey))
                    );
                    Tab tab = new Tab(cell.getItem());
                    tab.setContent(tb);
                    tabPane.getTabs().add(tab);
                    tabPane.getSelectionModel().select(tab);
                }
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
        MenuItem importFile = new MenuItem("Import *.ed file");
        importFile.setGraphic(new FontIcon("bi-cloud-upload-fill"));
        importFile.setOnAction(e -> {
            File file = fileChooser();
            var x = BasicParser.parse(file.getAbsolutePath(), String.valueOf(invertedMap.get(name)));
            allQuestions.put(invertedMap.get(name), x);
            var newChild = new ContextTreeItem("Draft_" + LocalDate.now() + "_" + LocalTime.now().
                    format(DateTimeFormatter.ofPattern("HH:mm:ss")), contextMenu);
            saveToDb.setOnAction(event -> {
                CompletableFuture.runAsync(() -> {
                    for (var temp : x.keySet()) {
                        var question = x.get(temp);
                        try {
                            addQuestion(question);
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
            });
            FontIcon node = new FontIcon();
            newChild.setGraphic(
                    node);
            node.getStyleClass().add("ikonli-font-icon");
            node.setStyle("""
                    -fx-icon-code:mdi2f-folder;
                    -fx-icon-color:#944a00;
                    """);
            for (int i = 1; i <= x.size(); i++) {
                newChild.getChildren().add(
                        new TreeItem<>("Question " + i, new FontIcon("mdi2n-note")));
            }
            target.getChildren().add(newChild);
        });
        return new ContextMenu(addQuiz, importFile, rename, new SeparatorMenuItem(), delete);
    }

    private MenuItem getMenuItem(TreeItem<String> target, String name) {
        MenuItem addQuiz = new MenuItem("Add " + name + " Quiz");
        addQuiz.setGraphic(new FontIcon("mdi2n-note"));
        addQuiz.setOnAction(e -> {
            var newQuiz = mainController.gradedFxmlLoader.createView(R.newQuiz,
                    new NewQuiz(target, this, mainController, invertedMap.get(name)));
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
            while (rs.next()) {
                int topicId = rs.getInt("topic_id");
                int questionId = rs.getInt("question_id");
                if (mapOfQuestion.containsKey(topicId)) {
                    if (mapOfQuestion.get(topicId).containsKey(questionId)) {
                        mapOfQuestion.get(topicId).get(questionId).
                                option_data().options().put(rs.getInt("option_id"),
                                        rs.getString("option_text"));
                        if (rs.getInt("is_correct") == 1)
                            mapOfQuestion.get(topicId).get(questionId).
                                    option_data().setOption_index(mapOfQuestion.get(topicId).
                                            get(questionId).option_data().options().size() - 1);
                    } else {
                        mapOfQuestion.get(topicId).put(questionId, new QuestionData("" + questionId,
                                rs.getString("topic_id"),
                                rs.getString("user_id"),
                                rs.getString("date_of_making"),
                                rs.getString("type"),
                                rs.getString("level"),
                                rs.getString("question_txt"),
                                rs.getString("question_img_path"), new OptionData(
                                0, new LinkedHashMap<>(Map.of(rs.getInt("option_id"),
                                rs.getString("option_text"))))));
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
                            0, new LinkedHashMap<>(Map.of(rs.getInt("option_id"),
                            rs.getString("option_text"))))));
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
            case "dashboard" -> {
                topRightQuizView.setVisible(false);
                selectedTab.getStyleClass().clear();
                source.getStyleClass().add("tab_selector");
                selectedTab = source;
                previouslySelectedNode = quiz_gen_layout.getCenter();
                quiz_gen_layout.setCenter(mainController.gradedFxmlLoader.createView(R.exam_report, new ExamReport(mainController)));
            }
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
                           t.topic_name,
                           e.board
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
                String board = rs.getString("board");
                ExamData exam = new ExamData(
                        id,
                        classes,
                        doe,
                        room,
                        subject,
                        time,
                        topic_id,
                        topic_name,
                        board
                );
                examList.add(exam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return examList;
    }

    private LottiePlayer generateLottie() {
        final String resource = "/org/graded_classes/graded_attendance/css/Exams Preparation1.json";
        final Animation animationFile;
        try {
            File lottieJson = extractResourceToTempFile(resource, "motivation-", ".json");
            animationFile = LottieFileLoader.load(lottieJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Lottie animation from " + resource, e);
        }
        return new LottiePlayer(animationFile,
                animationFile.width(), animationFile.height());
    }

    public File fileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Option Image Selector");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.txt")
        );
        return fileChooser.showOpenDialog(mainController.getStage());
    }

    public int addQuestion(QuestionData question) throws SQLException {
        Connection conn = mainController.gradedDataLoader.
                databaseLoader.getConnection();
        String questionSql = """
                INSERT INTO Questions
                (
                    topic_id,
                    user_id,
                    date_of_making,
                    type,
                    level,
                    question_txt,
                    question_img_path
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String optionSql = """
                INSERT INTO QuestionOptions
                (
                    question_id,
                    option_text,
                    option_order,
                    is_correct
                )
                VALUES (?, ?, ?, ?)
                """;


        try {

            int questionId;

            // Insert Question
            try (PreparedStatement ps = conn.prepareStatement(
                    questionSql,
                    Statement.RETURN_GENERATED_KEYS
            )) {

                ps.setString(1, question.topic_id());
                ps.setString(2, question.user_id());
                ps.setString(3, question.date_of_making());
                ps.setString(4, question.type());
                ps.setString(5, question.level());
                ps.setString(6, question.question_txt());
                ps.setString(7, question.question_img_path());

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to get generated question id.");
                    }

                    questionId = rs.getInt(1);
                }
            }

            // Insert Options
            try (PreparedStatement ps = conn.prepareStatement(optionSql)) {

                int correctIndex = question.option_data().option_index();

                for (var entry : question.option_data().options().entrySet()) {

                    int order = entry.getKey();
                    String optionText = entry.getValue();

                    ps.setInt(1, questionId);
                    ps.setString(2, optionText);
                    ps.setInt(3, order);
                    ps.setInt(4, order == correctIndex ? 1 : 0);

                    ps.addBatch();
                }

                ps.executeBatch();
            }
            return questionId;

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {

        }
    }
}
