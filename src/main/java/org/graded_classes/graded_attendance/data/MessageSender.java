package org.graded_classes.graded_attendance.data;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.messaging.TelegramBot;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;


public class MessageSender {
    public MessageData message;
    TelegramBot bot;

    public MessageSender(DatabaseLoader databaseLoader, MainController mainController, String token) {
        message = new MessageData(databaseLoader, mainController);
        Platform.runLater(() -> {
            try {
                TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
                bot = new TelegramBot(token);
                botsApplication.registerBot(token, bot);
                bot.setMessageData(message);
                System.out.println("MyAmazingBot successfully started!");
            } catch (TelegramApiException e) {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeight(300);
                alert.setHeaderText("No internet connection");
                alert.setContentText("Please check your internet connection");
                alert.show();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    public void sendMessage(String message, long id) {
        bot.sendText(id, message);
    }

    public void sendImage(File file, long id) {
        bot.sendImage(id, file);
    }
}
