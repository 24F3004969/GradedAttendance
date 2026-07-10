package org.graded_classes.graded_attendance.data;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import org.graded_classes.graded_attendance.controller.home.MainController;

import java.sql.Statement;

public class MessageData {
    Statement statement;
    DatabaseLoader databaseLoader;
    MainController mainController;

    public boolean isThisStudentPresent(String edNo) {
        return mainController.gradedDataLoader.studentData.containsKey(edNo) && mainController.gradedDataLoader.studentData.get(edNo).telegram_id() != null &&
                !mainController.gradedDataLoader.studentData.get(edNo).telegram_id().trim().isEmpty();
    }

    public MessageData(DatabaseLoader databaseLoader, MainController mainController) {
        this.mainController = mainController;
        this.databaseLoader = databaseLoader;
        statement = databaseLoader.getStatement();
    }

    public boolean updateTelegramId(String edNo, String name, String _class, String newTelegramId) {

        var rowsAffected = mainController.gradedDataLoader.
                getStudentData().get(edNo).updateTelegram(databaseLoader.getConnection(),
                        edNo, name, _class, newTelegramId);
        if (rowsAffected > 0) {
            Platform.runLater(() -> mainController.sendNotification(name + " was added to graded messaging system", Styles.SUCCESS));
            mainController.gradedDataLoader.getStudentData().
                    get(edNo).setTelegram_id(newTelegramId);
            return true;
        } else {
            return false;
        }
    }
}
