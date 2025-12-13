package org.graded_classes.graded_attendance.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.R;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.graded_classes.graded_attendance.GradedResourceLoader.loadURL;

public class StudentFeeLayout extends FeeDataView implements Initializable {
    @FXML
    private TextField amount_to_pay;
    @FXML
    GridPane monthsGrid;
    @FXML
    private Label current_date;

    @FXML
    private Label day_and_time;

    @FXML
    private TextField due_amount;

    @FXML
    private TextField ed_no;

    @FXML
    private ComboBox<String> mode;

    @FXML
    private TextField name_of_receiver;
    @FXML
    Spinner<Integer> years;
    private Button selectedMonth;
    String ed;
    String previous;
    @FXML
    SplitPane splitPane;
    MainController mainController;
    Node paymentNode;
    String name;

    public StudentFeeLayout(MainController mainController, String ed, String name) {
        super(mainController.gradedDataLoader, ed);
        this.mainController = mainController;
        this.ed = ed;
        this.name = name;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        current_date.setText(LocalDate.now().getDayOfMonth() + " " +
                format(LocalDate.now().getMonth().toString()) +
                " " + LocalDate.now().getYear());
        mode.setItems(FXCollections.observableArrayList(List.of("Online", "Offline")));
        ed_no.setText(ed);
        if (paymentNode != null) {
            splitPane.getItems().set(1, paymentNode);
        }
        updateColorCode();
        startClock();
    }

    private void updateColorCode() {
        String[][] months = feeRecords.get(ed);
        boolean found = false;
        for (int i = 0; i < LocalDate.now().getMonth().getValue(); i++) {
            var arr = months[i];
            if (notAnyNull(arr)) {
                Button button = (Button) monthsGrid.getChildren().get(i);
                button.getStylesheets().clear();
                button.getStylesheets().add(loadURL("css/paid.css").toExternalForm());
                found = true;
            } else if (found) {
                Button button = (Button) monthsGrid.getChildren().get(i);
                button.getStylesheets().clear();
                button.getStylesheets().add(loadURL("css/unpaid.css").toExternalForm());
            }
        }
    }

    private boolean notAnyNull(String[] arr) {
        return arr[0] != null && arr[1] != null && arr[2] != null && arr[3] != null;
    }

    private ArrayList<Integer> getList() {
        return IntStream.rangeClosed(1990, 2100).
                boxed().collect(Collectors.toCollection(ArrayList::new));
    }

    private String format(String date) {
        return date.charAt(0) + date.substring(1).toLowerCase();
    }

    @FXML
    void cancel(ActionEvent event) {

    }

    @FXML
    void onMonthClicked(ActionEvent event) {
        Button button = ((Button) event.getSource());
        if (!button.getStylesheets().isEmpty()) {
            var sheet = button.getStylesheets().getLast();
            sheet = sheet.substring(sheet.lastIndexOf('/') + 1);
            System.out.println(sheet);
            if (!sheet.equals("paid.css")) {
                change(event);
                if (paymentNode != null) {
                    splitPane.getItems().set(1, paymentNode);
                }
            }
        } else {
            change(event);
            if (paymentNode != null) {
                splitPane.getItems().set(1, paymentNode);
            }
        }
    }

    private void change(ActionEvent event) {
        if (selectedMonth != null) {
            selectedMonth.getStylesheets().removeLast();
        }
        selectedMonth = (Button) event.getSource();
        selectedMonth.getStylesheets().add(loadURL("css/selectButton.css").toExternalForm());
    }

    @FXML
    void pay(ActionEvent event) {
        String[] s = {mode.getValue(), amount_to_pay.getText(), name_of_receiver.getText(), due_amount.getText().isEmpty() ? "0" : due_amount.getText()};
        if (nonEmptyOrNull(s) && selectedMonth != null) {
            String sql = """
                     UPDATE "fee_2025" SET %s = ? WHERE ed_no = ?
                    """.formatted(selectedMonth.getText().trim());
            Connection conn = gradedDataLoader.databaseLoader.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, Arrays.toString(s));
                stmt.setString(2, ed);
                stmt.executeUpdate();
                var alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Payment Confirmation");
                alert.setHeaderText("Payment done for " + ed);
                alert.show();
                selectedMonth.getStylesheets().add(loadURL("css/paid.css").toExternalForm());
                paymentNode = splitPane.getItems().getLast();
                splitPane.getItems().set(1, mainController.gradedFxmlLoader.createView(R.payment_done_animation));
                if (mainController.gradedDataLoader.getStudentData().get(ed).telegram_id() != null &&
                        !mainController.gradedDataLoader.getStudentData().get(ed).telegram_id().isEmpty())
                    mainController.messageSender.sendMessage("""
                             Fee Received
                            \s
                             Dear Parent,
                             We have received the fee for the month %s\s
                             Name : %s.
                             Amount : %s
                             Date : %s
                            \s
                             Thank you!
                             — Graded coaching classes
                            \s""".formatted(selectedMonth.getText(), name, amount_to_pay.getText(), LocalDate.now()), Long.parseLong(mainController.gradedDataLoader.getStudentData().get(ed).telegram_id()));

                selectedMonth = null;
                updateLastPaymentDate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Some data is missing.");
            alert.show();
        }
    }

    private void updateLastPaymentDate() {
        Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
        String sql = "UPDATE StudentData SET last_payment_date = ? WHERE ed_no = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, LocalDate.now().toString());
            pst.setString(2, ed_no.getText());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean nonEmptyOrNull(String[] s) {
        return s[0] != null && !s[0].isEmpty() && s[1] != null && !s[1].isEmpty() && s[2] != null && !s[2].isEmpty()
                && s[3] != null && !s[3].isEmpty();
    }

    public void startClock() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0), event -> {
                    LocalDateTime now = LocalDateTime.now();

                    // Format: Mon, 01:49:23 AM
                    String day = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    String time = now.format(DateTimeFormatter.ofPattern("hh:mm:ss a"));

                    day_and_time.setText(day + ", " + time);
                }),
                new KeyFrame(Duration.seconds(1)) // Update every second
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
