package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.EnhancedPasswordField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.util.ResourceBundle;


public class StudentExamLogin implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        studentEDNo.setText(examLogin.rollCode);
    }

    MainController mainController;
    Stage stage;
    ExamLogin examLogin;
    @FXML
    private EnhancedPasswordField pass;

    @FXML
    private TextField studentEDNo;
    public StudentExamLogin(MainController mainController, Stage stage, ExamLogin examLogin) {
        this.mainController = mainController;
        this.stage = stage;
        this.examLogin = examLogin;
    }

    @FXML
    private void login() {
        var taker = new QuizTaker(mainController,this);
        Parent view = (Parent) mainController.gradedFxmlLoader.createView(R.quiz_taker, taker);
        stage.getScene().setRoot(view);
        stage.setTitle("Exam");
        stage.setFullScreen(true);
        taker.startQuiz(examLogin, stage);

    }


}
