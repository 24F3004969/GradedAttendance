package org.graded_classes.graded_attendance.data;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import org.graded_classes.graded_attendance.controller.home.MainController;

import java.sql.Statement;
import java.util.Locale;

public class MessageData {
    Statement statement;
    DatabaseLoader databaseLoader;
    public MainController mainController;

    public boolean isThisStudentPresent(String edNo) {
        return mainController.gradedDataLoader.studentData.containsKey(edNo) && mainController.gradedDataLoader.studentData.get(edNo).telegram_id() != null &&
                !mainController.gradedDataLoader.studentData.get(edNo).telegram_id().trim().isEmpty();
    }

    public MessageData(DatabaseLoader databaseLoader, MainController mainController) {
        this.mainController = mainController;
        this.databaseLoader = databaseLoader;
        statement = databaseLoader.getStatement();
    }

    public boolean updateTelegramId(String edNo,String _class, String newTelegramId) {
        Student student = mainController.gradedDataLoader.
                getStudentData().get(edNo.trim().toUpperCase());
        var rowsAffected = student.updateTelegram(databaseLoader.getConnection(),
                        edNo,  _class, newTelegramId);

        if (rowsAffected > 0) {
            Platform.runLater(() -> mainController.sendNotification(student.name() + " was added to graded messaging system", Styles.SUCCESS));
            mainController.gradedDataLoader.getStudentData().
                    get(edNo.toUpperCase()).setTelegram_id(newTelegramId);
            return true;
        } else {
            return false;
        }
    }
}
