package org.graded_classes.graded_attendance.messaging;


import org.graded_classes.graded_attendance.data.MessageData;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.util.List;

public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {
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
                .chatId(who.toString())//Who are we sending a message to
                .text(what).build();    //Message content
     /*   SendPhoto myPhoto = SendPhoto.builder().
                chatId("6749377036").
                photo(new InputFile(new File("C:\\Users\\hilal\\GradedAttendance\\icon.png"))).caption("Graded Icon")
                .build();*/
        ;
        try {
            telegramClient.execute(sm);
           // telegramClient.execute(myPhoto);//Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

    @Override
    public void consume(List<Update> updates) {
        LongPollingSingleThreadUpdateConsumer.super.consume(updates);
    }

    @Override
    public void consume(Update update) {
        System.out.println(update);
        var msg = update.getMessage();
        var user = msg.getFrom();
        String[] test = msg.getText().split(",");
        if (test.length == 3) {
            if (messageData.isThisStudentPresent(test[0].trim())) {
                sendText(user.getId(), "You are already added to the messaging system.\nIn the case you want to change it contact the admin or in the office at Graded.\nGood luck, and thank you for your patience!");
                return;
            }
            boolean c = this.messageData.updateTelegramId(test[0], test[1], test[2], String.valueOf(user.getId()));
            if (c) {

                sendText(user.getId(), "🎉🎉🎊🎊Congratulations! " + test[0] + "\nName:" + test[1] + "\nClass:" + test[2] +
                        "\nYou have been successfully added to the Graded Coaching Classes Messaging System.\n" +
                        "We’re excited to have you on board—get ready to achieve great things!");
            } else {
                sendText(user.getId(), """
                        We're really sorry 🙏🙏
                        Something went wrong. Please try again.
                        It looks like there might be an issue with the roll number, name, or class you entered.
                        Please make sure they match exactly with the details you provided during admission.
                        Good luck, and thank you for your patience!
                        """);
            }
        } else
            sendText(user.getId(), "Invalid format: Right formate is [ed_no,name,class]\nFor example if your roll is ED01 and name is Helal Anwar and class is X.\nThen you should type ED01,Helal Anwar,X");
    }
}

