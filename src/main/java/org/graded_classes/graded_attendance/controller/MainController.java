package org.graded_classes.graded_attendance.controller;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.GradedFxmlLoader;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.calender.CalendarApp;
import org.graded_classes.graded_attendance.data.Formatter;
import org.graded_classes.graded_attendance.data.GradedDataLoader;
import org.graded_classes.graded_attendance.data.MessageSender;
import org.graded_classes.graded_attendance.data.Student;
import org.graded_classes.graded_attendance.planner.Planner;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    VBox notificationsVBox, notificationBox;
    ScrollPane notificationsScrollPane;
    @FXML
    public StackPane stackPane;
    @FXML
    HBox selectedTab;
    Tooltip tooltip;
    Stage stage;
    Node home, chat, calendar, lesson;
    Map<String, Image> toggleInImages = Map.of(
            "home", new Image(GradedResourceLoader.load("icons/home_in.svg")),
            "chat", new Image(GradedResourceLoader.load("icons/chat_in.svg")),
            "calender", new Image(GradedResourceLoader.load("icons/calendar_month_in.svg")),
            "database", new Image(GradedResourceLoader.load("icons/database_in.svg")),
            "lesson", new Image(GradedResourceLoader.load("icons/book_in.svg")));
    Map<String, Image> toggleOutImages = Map.of(
            "home", new Image(GradedResourceLoader.load("icons/home.svg")),
            "chat", new Image(GradedResourceLoader.load("icons/chat.svg")),
            "calender", new Image(GradedResourceLoader.load("icons/calendar_month.svg")),
            "database", new Image(GradedResourceLoader.load("icons/database.svg")),
            "lesson", new Image(GradedResourceLoader.load("icons/book.svg")));
    @FXML
    ModalPane modalPane;
    GradedFxmlLoader gradedFxmlLoader = new GradedFxmlLoader();
    @FXML
    BorderPane main_view;
    public GradedDataLoader gradedDataLoader = new GradedDataLoader(this);
    MessageSender messageSender;

    public MainController(Stage stage) {
        this.stage = stage;

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        home = gradedFxmlLoader.createView(R.home_layout, new HomeController(modalPane,
                gradedDataLoader, this));
        chat = gradedFxmlLoader.createView(R.chat_layout, new ChatController(this));
        calendar = new CalendarApp().createCalenderView();
        main_view.setCenter(navigateView("home"));
        tooltip = new Tooltip(Formatter.format(selectedTab.getId()));
        Tooltip.install(selectedTab, tooltip);
        messageSender = new MessageSender(gradedDataLoader.databaseLoader, this, getToken());
        notificationInit();
        ArrayList<Student> students = getStudentsWithFeeDateIsWeekAfter();
        if (!students.isEmpty()) {
            sendNotification("Some students with ED No. .... have there fee dues date in a week", Styles.ACCENT, students);
        }
    }

    private ArrayList<Student> getStudentsWithFeeDateIsWeekAfter() {
        ArrayList<Student> students = new ArrayList<>();
        for (Student student : gradedDataLoader.getStudentData().values()) {
            if (!student.getLastPaymentDate().isEmpty()) {
                long daysDifference = getFeeDueDays(student);
                if (daysDifference >= 0 && daysDifference <= 7) {
                    students.add(student);
                } else if (daysDifference < 0) {
                    students.add(student);
                }
            }
        }
        return students;
    }

    private static long getFeeDueDays(Student student) {
        LocalDate date1 = LocalDate.parse(student.getLastPaymentDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        var deadline = date1.plusDays(30);
        return ChronoUnit.DAYS.between(LocalDate.now(), deadline);
    }

    public String getToken() {
        String query = "SELECT id FROM token LIMIT 1";

        try (PreparedStatement stmt = gradedDataLoader.databaseLoader.getConnection().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // No token found or error occurred
    }


    private void notificationInit() {
        notificationBox = (VBox) gradedFxmlLoader.createView(R.notification);
        notificationsScrollPane = (ScrollPane) notificationBox.getChildren().getFirst();
        StackPane.setAlignment(notificationBox, Pos.TOP_RIGHT);
        StackPane.setMargin(notificationBox, new Insets(5, 5, 0, 0));
        notificationsVBox = (VBox) notificationsScrollPane.getContent();
        Button clearAllButton = (Button) ((HBox) notificationBox.getChildren().get(1)).getChildren().getFirst();
        clearAllButton.setOnAction(_ -> {
            for (int i = 0; i < notificationsVBox.getChildren().size(); i++) {
                var node = notificationsVBox.getChildren().get(i);
                var out = Animations.slideOutRight(node, Duration.millis(500));
                out.setOnFinished(_ -> {
                    notificationsVBox.getChildren().remove(node);
                    if (notificationsVBox.getChildren().isEmpty()) {
                        notificationsScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
                        stackPane.getChildren().remove(notificationBox);
                    }
                });
                out.playFromStart();
            }
        });
    }

    @FXML
    void navigation(MouseEvent event) {
        HBox root = (HBox) event.getSource();
        ImageView imageView = ((ImageView) root.getChildren().getLast());
        Rectangle rectangle = (Rectangle) root.getChildren().getFirst();
        toggleOut(selectedTab, (Rectangle) selectedTab.getChildren().getFirst(),
                (ImageView) selectedTab.getChildren().getLast());
        toggleIn(root, rectangle, imageView);
        main_view.setCenter(navigateView(root.getId()));
        Tooltip.uninstall(selectedTab, tooltip);
        selectedTab = root;
        tooltip.setText(Formatter.format(selectedTab.getId()));
        Tooltip.install(selectedTab, tooltip);
    }

    private Node navigateView(String id) {
        return switch (id) {
            case "home" -> home;
            case "chat" -> chat;
            case "calender" -> calendar;
            case "database" -> gradedFxmlLoader.createView(R.database_layout, new DataBaseController(this));
            case "lesson" -> gradedFxmlLoader.createView(R.lesson_planner, new Planner(gradedDataLoader));
            default -> null;
        };
    }

    private void toggleIn(HBox root, Rectangle rectangle, ImageView imageView) {
        root.setStyle("-fx-background-color:  #1C75BC;-fx-background-radius: 0 5 5 0;");
        rectangle.setFill(Paint.valueOf("#fafafa"));
        imageView.setImage(toggleInImages.get(root.getId()));
    }

    private void toggleOut(HBox root, Rectangle rectangle, ImageView imageView) {
        root.setStyle("-fx-background-color:transparent;");
        rectangle.setFill(Paint.valueOf("#fafafa00"));
        imageView.setImage(toggleOutImages.get(root.getId()));
    }

    public void sendNotification(String message, String styles) {
        if (!stackPane.getChildren().contains(notificationBox)) {
            stackPane.getChildren().add(notificationBox);
        }
        var msg = new Notification(message, new FontIcon(MaterialDesignH.HELP_CIRCLE_OUTLINE));
        msg.getStyleClass().add(styles);
        msg.setPrefHeight(Region.USE_PREF_SIZE);
        msg.setMaxHeight(Region.USE_PREF_SIZE);
        msg.setOnClose(_ -> {
            var out = Animations.slideOutUp(msg, Duration.millis(250));
            out.setOnFinished(_ -> {
                notificationsVBox.getChildren().remove(msg);
                if (notificationsVBox.getChildren().size() <= 5) {
                    notificationsScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);

                }
                if (notificationsVBox.getChildren().isEmpty()) {
                    notificationsScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    stackPane.getChildren().remove(notificationBox);

                }
            });
            out.playFromStart();
        });
        var in = Animations.slideInDown(msg, Duration.millis(250));
        if (!notificationsVBox.getChildren().contains(msg)) {
            VBox.setMargin(msg, new Insets(2));
            if (notificationsVBox.getChildren().size() >= 5) {
                notificationsScrollPane.setPrefHeight(300);
            }
            notificationsVBox.getChildren().addAll(msg);

        }
        in.playFromStart();
    }

    public void sendNotification(String message, String styles, ArrayList<Student> students) {
        if (!stackPane.getChildren().contains(notificationBox)) {
            stackPane.getChildren().add(notificationBox);
        }
        var msg = new Notification(message, new FontIcon(MaterialDesignH.HELP_CIRCLE_OUTLINE));
        msg.setPrimaryActions(new Button("Send Notification"));
        msg.getPrimaryActions().getFirst().setOnMouseClicked(event -> {
            for (Student student : students) {
                try {
                    long daysDifference = getFeeDueDays(student);
                    if (daysDifference >= 0) {
                        messageSender.sendMessage("Your fee due date is " + LocalDate.now().plusDays(daysDifference) + ".\nPlease pay on time as this helps us to deliver the best" +
                                " possible coaching experience/uninterrupted service you expect.", Long.parseLong(student.telegram_id()));
                    }
                    else{
                        messageSender.sendMessage("Your fee due date has passed.Please pay by" + LocalDate.now().plusDays(1) + ".\nPlease pay on time as this helps us to deliver the best" +
                                " possible coaching experience/uninterrupted service you expect.", Long.parseLong(student.telegram_id()));
                    }
                } catch (Exception e) {
                    System.err.println("Failed to send notification " + student.ed_no() + " class :" + student._class() + " name : " + student.name());
                }
            }
        });
        msg.getStyleClass().add(styles);
        msg.setPrefHeight(Region.USE_PREF_SIZE);
        msg.setMaxHeight(Region.USE_PREF_SIZE);
        msg.setOnClose(_ -> {
            var out = Animations.slideOutUp(msg, Duration.millis(250));
            out.setOnFinished(_ -> {
                notificationsVBox.getChildren().remove(msg);
                if (notificationsVBox.getChildren().size() <= 5) {
                    notificationsScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);

                }
                if (notificationsVBox.getChildren().isEmpty()) {
                    notificationsScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    stackPane.getChildren().remove(notificationBox);

                }
            });
            out.playFromStart();
        });
        var in = Animations.slideInDown(msg, Duration.millis(250));
        if (!notificationsVBox.getChildren().contains(msg)) {
            VBox.setMargin(msg, new Insets(2));
            if (notificationsVBox.getChildren().size() >= 5) {
                notificationsScrollPane.setPrefHeight(300);
            }
            notificationsVBox.getChildren().addAll(msg);

        }
        in.playFromStart();
    }

    public void onSetting() {

    }
}
