package org.graded_classes.graded_attendance.controller.quiz;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;

public class StudentExamLogin {
    MainController mainController;
    Stage stage;
    ExamLogin examLogin;

    public StudentExamLogin(MainController mainController, Stage stage, ExamLogin examLogin) {
        this.mainController = mainController;
        this.stage = stage;
        this.examLogin = examLogin;
    }

    @FXML
    private void login() {
        var taker = new QuizTaker(mainController);
        Parent view = (Parent) mainController.gradedFxmlLoader.createView(R.quiz_taker, taker);
        //var par = (StackPane) examLogin.root.getParent();
        //par.getChildren().remove(examLogin.root);
        stage.getScene().setRoot(view);
        stage.setTitle("Exam");
        stage.setFullScreen(true);
        taker.startQuiz(examLogin, stage);

    }


}
