package org.graded_classes.graded_attendance.controller.quiz;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class QuizTaker implements Initializable {

    @FXML
    private ToggleGroup ans;

    @FXML
    private TilePane questionNum;
    @FXML
    private Label question_num;
    @FXML
    private Label quizName;
    @FXML
    private VBox questionStack;
    @FXML
    private Label timer;
    int indexOfQuestion = 0;
    MainController mainController;
    ArrayList<QuestionData> questionList = new ArrayList<>();
    ArrayList<SplitPane> listOfSplitPane = new ArrayList<>();

    public QuizTaker(MainController mainController) {
        this.mainController = mainController;
        CompletableFuture.runAsync(this::initDb);
    }

    private void initDb() {
        try {
            var connection = mainController.gradedDataLoader.databaseLoader.getConnection();
            var sql = "select * from Questions where user_id=1";
            PreparedStatement pst = connection.prepareStatement(sql);
            var rs = pst.executeQuery();
            while (rs.next()) {
                var inner_sql = "Select * from QuestionOptions where question_id=?";
                PreparedStatement _pst = connection.prepareStatement(inner_sql);
                _pst.setString(1, rs.getString("question_id"));
                var _rs = _pst.executeQuery();
                OptionData questionOption = null;
                ArrayList<String> options = new ArrayList<>();
                int correctId = 0;
                while (_rs.next()) {
                    options.add(_rs.getString("option_text") );
                    var id = _rs.getInt("is_correct");
                    if (id == 1)
                        correctId = options.size() - 1;
                }
                questionOption = new OptionData(correctId, options);
                QuestionData questionData = new QuestionData(rs.getString("question_id"),
                        rs.getString("topic_id"),
                        rs.getString("user_id"),
                        rs.getString("date_of_making"),
                        rs.getString("type"),
                        rs.getString("level"),
                        rs.getString("question_txt"),
                        rs.getString("question_img_path"), questionOption);
                questionList.add(questionData);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void nextQuestion() {
        var index = indexOfQuestion < listOfSplitPane.size()-1 ?
                ++indexOfQuestion : listOfSplitPane.size() - 1;
        VBox.setVgrow(listOfSplitPane.get(index), Priority.ALWAYS);
        questionStack.getChildren().set(1, listOfSplitPane.get(index));
        question_num.setText("Question " + (index + 1));
    }

    @FXML
    void onQuizSubmit(ActionEvent event) {

    }

    @FXML
    void previousQuestion() {
        var index = indexOfQuestion > 0 ? --indexOfQuestion : 0;
        VBox.setVgrow(listOfSplitPane.get(index), Priority.ALWAYS);
        questionStack.getChildren().set(1, listOfSplitPane.get(index));
        question_num.setText("Question " + (index + 1));
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (QuestionData questionData : questionList) {
            var qp = new QuestionTakerWthOp(questionData);
            var qop = (SplitPane) mainController.gradedFxmlLoader.createView(R.question_taker_with_op, qp);
            listOfSplitPane.add(qop);
        }
        VBox.setVgrow(listOfSplitPane.getFirst(), Priority.ALWAYS);
        questionStack.getChildren().set(1, listOfSplitPane.getFirst());
        for (int i = 0; i < questionList.size(); i++) {
            Button button = new Button();
            button.setText((i + 1) + "");
            button.setPrefHeight(50);
            button.setPrefWidth(50);
            questionNum.getChildren().add(button);
        }
        System.out.println();
    }
}
