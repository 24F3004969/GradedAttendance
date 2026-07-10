package org.graded_classes.graded_attendance.controller.quiz;


import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.ExamData;
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class ConductExam implements Initializable {
    MainController mainController;

    @FXML
    private TableColumn<ExamData, HBox> action;

    @FXML
    private TableColumn<ExamData, String> classes, doe, id, room, subject, time, topic;
    @FXML
    private TableView<ExamData> scheduleTable;
    ObservableList<ExamData> items = FXCollections.observableList(new ArrayList<>());

    @FXML
    private Label today;
    private final ArrayList<ExamData> examSchedular;

    public ConductExam(MainController mainController, ArrayList<ExamData> examSchedular) {
        this.mainController = mainController;
        this.examSchedular = examSchedular;
    }

    @FXML
    void addNewExam() {
        mainController.modalPane.setPersistent(true);
        Node node = mainController.gradedFxmlLoader.createView(R.exam_create, new ExamCreator(mainController, this));
        mainController.modalPane.show(node);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        today.setText(LocalDate.now().getDayOfMonth() + " " +
                format(LocalDate.now().getMonth().toString()) +
                " " + LocalDate.now().getYear());
        classes.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().classes()));
        doe.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().doe()));
        id.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().id()));
        room.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().room()));
        subject.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().subject()));
        time.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().time()));
        topic.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().topic_name()));
        action.setCellValueFactory(arg0 -> {
            Button button = new Button("Start Exam");
            Button result = new Button("Generate Result");
            button.setPadding(new Insets(5, 5, 5, 5));
            button.getStyleClass().add(Styles.SUCCESS);
            result.setPadding(new Insets(5, 5, 5, 5));
            result.getStyleClass().add(Styles.SUCCESS);
            ExamData examInfo = arg0.getValue();
            HBox hBox = new HBox(button, result);
            hBox.setAlignment(Pos.CENTER);
            hBox.setSpacing(5);
            button.setOnMouseClicked(_ -> mainController.modalPane.show(mainController.
                    gradedFxmlLoader
                    .createView(R.exam_entry_login,
                            new LoginBeforeEntry(mainController, examInfo))));
            result.setOnMouseClicked(_ -> {
                generateResult(examInfo);
            });
            return new SimpleObjectProperty<>(hBox);
        });
        for (var sec : examSchedular) {
            items.addAll(sec);
        }
        scheduleTable.setItems(items);
    }

    private void generateResult(ExamData examInfo) {
        var listOfStudents = mainController.gradedDataLoader.getStudentData().
                sequencedValues().stream().filter(student -> student._class().
                        equals(examInfo.classes())).toList();
        System.out.println("Exam Result , Subject: " + examInfo.subject() + " ,Class: " + examInfo.classes());
        System.out.println("Conducted on " + examInfo.doe() + " , Topic Name:  " + examInfo.topic_name());
        var questionDataMap = loadQuestion(examInfo);
        for (var st : listOfStudents) {
            int score = 0;
            var map = getAnswers(Integer.parseInt(examInfo.id()), st.ed_no());
            for (var n : map.keySet()) {
                int opId = map.get(n);
                var question = questionDataMap.get("" + n);
                if (question.option_data() != null && question.option_data().option_index() == opId)
                    score = score + 4;
            }
            System.out.println(st.ed_no() + ":    " + ((score <= 0) ? "Absent or not eligible" : score+"/80"+"       Name: "+st.name()));
        }

    }

    private TreeMap<String, QuestionData> loadQuestion(ExamData examData) {
        TreeMap<String, QuestionData> map = new TreeMap<>();
        try {
            var connection = mainController.gradedDataLoader.databaseLoader.getConnection();
            var sql = """
                    select *
                    from ExamQuestion
                             join Questions on Questions.question_id = ExamQuestion.question_id
                    where exam_id = %s
                    """.
                    formatted(examData.id());
            java.sql.ResultSet rs;
            try {
                PreparedStatement pst = connection.prepareStatement(sql);
                rs = pst.executeQuery();
                while (rs.next()) {
                    var inner_sql = "Select * from QuestionOptions where question_id=?";
                    PreparedStatement _pst = connection.prepareStatement(inner_sql);
                    String questionId = rs.getString("question_id");
                    _pst.setString(1, questionId);
                    var _rs = _pst.executeQuery();
                    OptionData questionOption = null;
                    ArrayList<String> options = new ArrayList<>();
                    int correctId = 0;
                    while (_rs.next()) {
                        options.add(_rs.getString("option_text"));
                        var id = _rs.getInt("is_correct");
                        if (id == 1)
                            correctId = options.size();
                    }
                    questionOption = new OptionData(correctId, options);
                    QuestionData questionData = new QuestionData(questionId,
                            rs.getString("topic_id"),
                            rs.getString("user_id"),
                            rs.getString("date_of_making"),
                            rs.getString("type"),
                            rs.getString("level"),
                            rs.getString("question_txt"),
                            rs.getString("question_img_path"), questionOption);
                    map.put(questionData.question_id(), questionData);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return map;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public static TreeMap<Integer, Integer> getAnswers(int examId, String studentEd) {

        TreeMap<Integer, Integer> answersMap = new TreeMap<>();
        String DB_URL = "jdbc:sqlite:" + "G:/My Drive/GradeEd_Exam_2026/" + studentEd + ".db";

        String sql = "SELECT question_id, selected_option_id FROM answers WHERE exam_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, examId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int questionId = rs.getInt("question_id");
                int selectedOptionId = rs.getInt("selected_option_id");

                answersMap.put(questionId, selectedOptionId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return answersMap;
    }

    private String format(String date) {
        return date.charAt(0) + date.substring(1).toLowerCase();
    }
}

