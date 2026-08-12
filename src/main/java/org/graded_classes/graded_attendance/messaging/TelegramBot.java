package org.graded_classes.graded_attendance.messaging;


import org.graded_classes.graded_attendance.AppMode;
import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.data.MessageData;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class TelegramBot extends DefaultLongPollingUpdateConsumer {
    MessageData messageData;
    private final TelegramClient telegramClient;

    public TelegramBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    public void setMessageData(MessageData messageData) {
        this.messageData = messageData;
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(Main.appMode == AppMode.DEV ? "6749377036" : who.toString())//Who are we sending a message to
                .text(what).build();
        try {
            telegramClient.execute(sm);
            // telegramClient.execute(myPhoto);//Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

    public void sendImage(Long who, File file) {
        SendPhoto myPhoto = SendPhoto.builder().
                chatId(Main.appMode == AppMode.DEV ? "6749377036" : who.toString()).
                photo(new InputFile(file)).caption("Fee Receipt")
                .build();
        try {
            telegramClient.execute(myPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

    public void sendDocument(Long who, File file, String caption) {
        SendDocument myDocument = SendDocument.builder().
                chatId(Main.appMode == AppMode.DEV ? "6749377036" : who.toString()).
                document(new InputFile(file)).caption(caption)
                .build();
        try {
            telegramClient.execute(myDocument);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

    @Override
    public void consume(List<Update> updates) {
        super.consume(updates);
    }

    @Override
    public void consume(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        String[] test = msg.getText().split(",");
        if (test.length == 2) {
            if (messageData.isThisStudentPresent(test[0].trim().toUpperCase())) {
                sendText(user.getId(), "You are already added to the messaging system.\nIn the case you want to change it contact the admin or in the office at Graded.\nGood luck, and thank you for your patience!");
            } else {
                messageData.mainController.approve(user, test, this);
            }
        } else
            sendText(user.getId(), "Invalid format: Right formate is [ed_no,class(Capital Roman number)]\nFor example if your roll is ED01 and class is X.\nThen you should type ED01,X");
    }

    public void extracted(String[] test, User user, boolean isApprove) {
        String name=messageData.mainController.gradedDataLoader.getStudentData().get(test[0].trim().toUpperCase(Locale.ROOT)).name();
        if (isApprove) {
            boolean c = this.messageData.updateTelegramId(test[0], test[1], String.valueOf(user.getId()));
            if (c) {

                sendText(user.getId(), "Congratulations! " + test[0] + "\nName:" + name + "\nClass:" + test[1] +
                        "\nYou have been successfully added to the Graded Coaching Classes Messaging System.\n" +
                        "We’re excited to have you on board—get ready to achieve great things!");
            } else {
                sendText(user.getId(), """
                        We're really sorry
                        Something went wrong. Please try again.
                        It looks like there might be an issue with the roll number, name, or class you entered.
                        Please make sure they match exactly with the details you provided during admission.
                        Good luck, and thank you for your patience!
                        """);
            }
        } else {
            sendText(user.getId(), """
                    Access DeniedWe apologize for the inconvenience.
                    Please contact the administrator at GradeEd Coaching Classes for assistance.
                    """);
        }
    }
}

