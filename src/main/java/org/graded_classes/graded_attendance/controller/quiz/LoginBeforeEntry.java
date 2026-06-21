package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.EnhancedPasswordField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.data.ExamData;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginBeforeEntry implements Initializable {
    MainController mainController;
    ExamData examInfo;

    public LoginBeforeEntry(MainController mainController, ExamData examInfo) {
        this.mainController = mainController;
        this.examInfo = examInfo;
    }

    @FXML
    private EnhancedPasswordField loginPassword;

    @FXML
    private TextField name;

    @FXML
    private TextField rollCode;

    @FXML
    private TextField seatNo;

    @FXML
    void close() {
        mainController.modalPane.hide();
    }

    @FXML
    void create() {
        if (validateBeforeLogin()) {
            ExamLogin examLogin = new ExamLogin(mainController, rollCode.getText(), name.getText(), seatNo.getText(),examInfo);
            examLogin.showLoginScreen();
        }
    }

    private boolean validateBeforeLogin() {
        if (rollCode.getText().isEmpty()) {
            rollCode.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            return false;
        }

        if (name.getText().isEmpty()) {
            name.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            return false;
        }
        if (seatNo.getText().isEmpty()) {
            seatNo.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            return false;
        }
        if (loginPassword.getText().isEmpty()) {
            loginPassword.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            return false;
        }
        return true;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}

