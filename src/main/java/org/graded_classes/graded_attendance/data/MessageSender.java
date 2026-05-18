package org.graded_classes.graded_attendance.data;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.messaging.TelegramBot;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.concurrent.CompletableFuture;


public class MessageSender {
    public MessageData message;
    private TelegramBot bot;

    public MessageSender(DatabaseLoader databaseLoader, MainController mainController, String token) {
        message = new MessageData(databaseLoader, mainController);
        CompletableFuture.runAsync(() -> {
            try {
                try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
                    bot = new TelegramBot(token);
                    long initTime = System.currentTimeMillis();
                    botsApplication.registerBot(token, bot);
                    long finalTime = System.currentTimeMillis();
                    System.out.println((finalTime - initTime) + " ms");
                    bot.setMessageData(message);

                    System.out.println("MyAmazingBot successfully started!");
                }
            } catch (TelegramApiException e) {
                Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("No internet connection");
                    alert.setContentText("Please check your internet connection");
                    alert.show();
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sendMessage(String message, long id) {
        CompletableFuture.runAsync(() -> bot.sendText(id, message));
    }

    public void sendImage(File file, long id) {
        CompletableFuture.runAsync(() -> bot.sendImage(id, file));
    }
}