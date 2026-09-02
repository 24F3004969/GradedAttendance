package org.graded_classes.graded_attendance.controller.home;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import com.dlsc.gemsfx.DialogPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.GradedFxmlLoader;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.calender.CalendarApp;
import org.graded_classes.graded_attendance.controller.chat.ChatController;
import org.graded_classes.graded_attendance.controller.dashboard.Dashboard;
import org.graded_classes.graded_attendance.controller.database.DataBaseController;
import org.graded_classes.graded_attendance.controller.quiz.QuizGenerator;
import org.graded_classes.graded_attendance.data.Formatter;
import org.graded_classes.graded_attendance.data.GradedDataLoader;
import org.graded_classes.graded_attendance.data.MessageSender;
import org.graded_classes.graded_attendance.data.Student;
import org.graded_classes.graded_attendance.controller.leaderboard.*;
import org.graded_classes.graded_attendance.controller.planner.Planner;
import org.graded_classes.graded_attendance.messaging.TelegramBot;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.telegram.telegrambots.meta.api.objects.User;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class MainController implements Initializable {
    VBox notificationsVBox, notificationBox;
    ScrollPane notificationsScrollPane;
    @FXML
    public StackPane stackPane;
    @FXML
    HBox selectedTab;
    @FXML
    private Button ham;
    Tooltip tooltip;

    public Stage getStage() {
        return stage;
    }

    Stage stage;
    Node home, chat, calendar, lesson, database,quizCreator,leaderboard;
    @FXML
    public ModalPane modalPane;
    public GradedFxmlLoader gradedFxmlLoader = new GradedFxmlLoader();
    @FXML
    public BorderPane main_view;
    public GradedDataLoader gradedDataLoader = new GradedDataLoader(this);
    public MessageSender messageSender;
    StudentDataLoader studentDataLoader;
    Leaderboard1 l1;
    SeatingPlan l2;

    public MainController(Stage stage) {
        this.stage = stage;

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        home = gradedFxmlLoader.createView(R.home_layout, new HomeController(modalPane,
                gradedDataLoader, this));
        chat = gradedFxmlLoader.createView(R.chat_layout, new ChatController(this));
        database = gradedFxmlLoader.createView(R.database_layout, new DataBaseController(this));
        lesson = gradedFxmlLoader.createView(R.lesson_planner, new Planner(gradedDataLoader, modalPane));
        quizCreator=gradedFxmlLoader.createView(R.quiz_creator, new QuizGenerator(this));
        calendar = new CalendarApp().createCalenderView();
        main_view.setCenter(navigateView("home"));
        ham.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT);
        tooltip = new Tooltip(Formatter.format(selectedTab.getId()));
        Tooltip.install(selectedTab, tooltip);
        messageSender = new MessageSender(gradedDataLoader.databaseLoader, this, getToken());
        studentDataLoader = new StudentDataLoader(gradedDataLoader.getStudentData());
        l1 = new Leaderboard1(studentDataLoader, this);
        l2 = new SeatingPlan(getSeatingPlan());
        leaderboard=gradedFxmlLoader.createView(R.points_table, new PointsTable(studentDataLoader,
                l1,
                l2,
                this));
        notificationInit();
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
        try {
            FontIcon fontIcon = ((FontIcon) root.getChildren().getLast());
            Rectangle rectangle = (Rectangle) root.getChildren().getFirst();
            toggleOut(selectedTab, (Rectangle) selectedTab.getChildren().getFirst(),
                    ((FontIcon) selectedTab.getChildren().getLast()));
            toggleIn(root, rectangle, fontIcon);
        } catch (Exception e) {

        }
        main_view.setCenter(navigateView(root.getId()));
        Tooltip.uninstall(selectedTab, tooltip);
        selectedTab = root;
        tooltip.setText(Formatter.format(selectedTab.getId()));
        Tooltip.install(selectedTab, tooltip);
    }

    private Node navigateView(String id) {
        return switch (id) {
            case "dashboard" -> gradedFxmlLoader.createView(R.dashboard, new Dashboard());
            case "home" -> home;
            case "chat" -> chat;
            case "calender" -> calendar;
            case "database" -> database;
            case "lesson" -> lesson;
            case "quizCreator" -> quizCreator;
            case "leaderboard" -> leaderboard;
            /* case "setting" -> gradedFxmlLoader.createView(R.quiz_taker);*/
            default -> null;
        };
    }

    private LinkedHashMap<String, ArrayList<DailyTimeTable>> getSeatingPlan() {
        LinkedHashMap<String, ArrayList<DailyTimeTable>> tables = new LinkedHashMap<>();
        try {
            var stmt = gradedDataLoader.databaseLoader.getConnection();
            String sql = "SELECT * FROM SLOT_%s".formatted(LocalDate.now().getDayOfWeek().toString().substring(0, 3));
            PreparedStatement pst = stmt.prepareStatement(sql);
            ResultSet r = pst.executeQuery();
            while (r.next()) {
                if (tables.containsKey(r.getString("Time"))) {
                    tables.get(r.getString("Time")).add(new DailyTimeTable(r.getString("Time"),
                            r.getString("Class"),
                            r.getString("Room_No"), r.getString("Subject"),
                            r.getString("Teacher")));
                } else {
                    ArrayList<DailyTimeTable> list = new ArrayList<>();
                    list.add(new DailyTimeTable(r.getString("Time"),
                            r.getString("Class"),
                            r.getString("Room_No"), r.getString("Subject"),
                            r.getString("Teacher")));
                    tables.put(r.getString("Time"), list);
                }
            }

        } catch (SQLException _) {

        }
        return tables;
    }

    private void toggleIn(HBox root, Rectangle rectangle, FontIcon fontIcon) {
        root.setStyle("-fx-background-color:#1C75BC;-fx-background-radius: 0 5 5 0;");
        rectangle.setFill(Paint.valueOf("#fafafa"));
        fontIcon.setStyle("""
                -fx-icon-size:24;
                -fx-icon-color:#fafafa;
                """);
    }

    private void toggleOut(HBox root, Rectangle rectangle, FontIcon fontIcon) {
        root.setStyle("-fx-background-color:transparent;");
        rectangle.setFill(Paint.valueOf("#fafafa00"));
        fontIcon.setStyle("""
                -fx-icon-color:#1C75BC;
                """);
    }

    public void approve(User user, String[] test, TelegramBot telegramBot) {
        String name = studentDataLoader.getStudentLinkedHashMap().get(test[0].trim().toUpperCase()).name();
        Platform.runLater(() -> {
            var x = sendNotification("Need and an approval for " + user.getFirstName() +
                    "." + test[0] + " " + name + " class " + test[1], Styles.SUCCESS);
            var but = new Button("Approve");
            but.getStyleClass().add(Styles.SUCCESS);
            x.setPrimaryActions(but);
            but.setOnMouseClicked(event -> {
                showWarningDialog("Are you sure you want to approve for " + user.getFirstName() +
                        ". " + test[0] + " " + name + " class " + test[1], telegramBot, test, user);
            });
        });

    }

    public Notification sendNotification(String message, String styles) {
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
        return msg;
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
                    } else {
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

    @FXML
    void g_Hamburger() {
        modalPane.usePredefinedTransitionFactories(Side.LEFT);
        VBox box = (VBox) gradedFxmlLoader.createView(R.navigation);
        Button button = (Button) box.lookup("#back_button");
        button.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT);
        button.setOnMouseClicked(event -> {
            modalPane.hide();
            modalPane.setAlignment(Pos.CENTER);
        });
        modalPane.show(box);
        modalPane.setAlignment(Pos.TOP_LEFT);
    }

    private void showWarningDialog(String message, TelegramBot telegramBot, String[] test, User user) {

        com.dlsc.gemsfx.DialogPane dialogPane = new com.dlsc.gemsfx.DialogPane();
        com.dlsc.gemsfx.DialogPane.Dialog<ButtonType> dialog =
                new com.dlsc.gemsfx.DialogPane.Dialog<>(dialogPane, DialogPane.Type.WARNING);
        dialog.setTitle("Approval Warning");
        dialog.setContentAlignment(Pos.CENTER);
        dialog.setSameWidthButtons(true);
        Label content = new Label(message);
        dialog.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL
        );
        dialog.setContent(new StackPane(content));
        stackPane.getChildren().add(dialogPane);
        content.setOnMouseClicked(e -> dialog.cancel());
        dialog.setOnClose(buttonType -> {
            if (buttonType == ButtonType.OK) {
                CompletableFuture.runAsync(() -> {
                    telegramBot.extracted(test, user, true);
                });
            } else if (buttonType == ButtonType.CANCEL) {
                CompletableFuture.runAsync(() -> {
                    telegramBot.extracted(test, user, false);
                });
            }
        });
        dialog.show();
    }
}
