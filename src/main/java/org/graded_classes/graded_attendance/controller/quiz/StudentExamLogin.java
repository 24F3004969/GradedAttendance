package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.EnhancedPasswordField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.home.MainController;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;


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
    Label error;
    @FXML
    TextField studentEDNo;
    String type;

    public StudentExamLogin(MainController mainController, Stage stage, ExamLogin examLogin, String type) {
        this.mainController = mainController;
        this.stage = stage;
        this.examLogin = examLogin;
        this.type = type;
    }

    @FXML
    private void login() {
        error.setVisible(false);

        String enteredDob = pass.getText();
        String dob = "";

        if ("Normal".equals(type)) {
            var student = mainController.gradedDataLoader
                    .getStudentData()
                    .get(studentEDNo.getText());

            if (student == null) {
                error.setText("Student not found");
                error.setVisible(true);
                return;
            }

            dob = student.dob();
        }

        if (!"Diagnostic".equals(type) && !dob.equals(enteredDob)) {
            error.setText("Incorrect Password");
            error.setVisible(true);
            return;
        }
        examLogin.root.getChildren().add(examLogin.loadingScreen);
        CompletableFuture.runAsync(()->{
            Platform.runLater(() -> {
                var taker = new QuizTaker(mainController, this, type);

                Parent view = (Parent) mainController.gradedFxmlLoader.createView(
                        R.quiz_taker,
                        taker
                );
                stage.getScene().setRoot(view);
                stage.setTitle("Exam");
                stage.setFullScreen(true);
                taker.startQuiz(examLogin, stage);
                examLogin.root.getChildren().remove(examLogin.loadingScreen);
            });
        });
    }


}
