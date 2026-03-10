package org.graded_classes.graded_attendance.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

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
    private TextField reference_no;
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
        ArrayList<String> listOfMonthPaid = getMonthForThisStudent(ed);
        var rMap = generateMonthMapInt();
        Map<Integer, String> map = generateMonthMap();
        for (int i = 0; i < 12; i++) {
            if (listOfMonthPaid.contains(map.get(i))) {
                Button button = (Button) monthsGrid.getChildren().get(i);
                button.getStylesheets().clear();
                button.getStylesheets().add(loadURL("css/paid.css").toExternalForm());
            } else if (!listOfMonthPaid.contains(map.get(i)) && rMap.get(map.get(i)) < rMap.get(listOfMonthPaid.getLast())) {
                Button button = (Button) monthsGrid.getChildren().get(i);
                button.getStylesheets().clear();
                button.getStylesheets().add(loadURL("css/unpaid.css").toExternalForm());
            }
        }

    }

    private Map<Integer, String> generateMonthMap() {
        var map = new HashMap<Integer, String>();
        map.put(0, "Jan");
        map.put(1, "Feb");
        map.put(2, "Mar");
        map.put(3, "Apr");
        map.put(4, "May");
        map.put(5, "Jun");
        map.put(6, "Jul");
        map.put(7, "Aug");
        map.put(8, "Sep");
        map.put(9, "Oct");
        map.put(10, "Nov");
        map.put(11, "Dec");
        return map;
    }

    private Map<String, Integer> generateMonthMapInt() {
        Map<String, Integer> map = new TreeMap<>();
        map.put("Jan", 0);
        map.put("Feb", 1);
        map.put("Mar", 2);
        map.put("Apr", 3);
        map.put("May", 4);
        map.put("Jun", 5);
        map.put("Jul", 6);
        map.put("Aug", 7);
        map.put("Sep", 8);
        map.put("Oct", 9);
        map.put("Nov", 10);
        map.put("Dec", 11);
        return map;
    }

    private ArrayList<String> getMonthForThisStudent(String ed) {
        final String sql = """
                SELECT month
                FROM fee_payments
                WHERE ed_no = ?
                """;

        ArrayList<String> months = new ArrayList<>();
        try (PreparedStatement ps = mainController.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {
            ps.setString(1, ed.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String m = rs.getString(1); // column "month"
                    if (m != null && !m.isBlank()) {
                        months.add(m);
                    }
                }
            }
        } catch (SQLException e) {
            // Wrap or rethrow as you prefer
            throw new RuntimeException("Failed to fetch months for ed_no=" + ed, e);
        }
        var map = generateMonthMapInt();
        months.sort(Comparator.comparing(map::get));
        return months;
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
    void pay() {
        if (selectedMonth == null) {
            showError("Some data is missing (no month selected).");
            return;
        }

        // Collect/validate UI inputs on FX thread
        final String edNo = ed;
        if (edNo == null || edNo.isBlank()) {
            showError("Missing ED number.");
            return;
        }

        final String rawMonth = selectedMonth.getText() == null ? "" : selectedMonth.getText().trim();
        final String mon = normalizeMonthToAbbr(rawMonth);
        if (mon == null || mon.isBlank()) {
            showError("Invalid month: " + rawMonth);
            return;
        }

        final String amountStr = amount_to_pay.getText() == null ? "" : amount_to_pay.getText().trim();
        final java.math.BigDecimal amount;
        try {
            amount = new java.math.BigDecimal(amountStr);
            if (amount.signum() < 0) {
                showError("Amount cannot be negative.");
                return;
            }
        } catch (NumberFormatException nfe) {
            showError("Invalid amount: " + amountStr);
            return;
        }

        final String collectedByName = name_of_receiver.getText().trim();
        if (collectedByName.isBlank()) {
            showError("Collected-by name is required.");
            return;
        }

        final String paymentMode = Optional.ofNullable(mode.getSelectionModel().getSelectedItem()).orElse("").trim();
        if (!(paymentMode.equals("Online") || paymentMode.equals("Offline"))) {
            showError("Payment mode must be 'Online' or 'Offline'.");
            return;
        }

        final String gateway = "UPI";
        final String referenceNo = reference_no.getText().isBlank() ? null : reference_no.getText().trim();
        final String dueAmount = "0";
        final java.time.LocalDate nextFeeDate = java.time.LocalDate.now().plusDays(30);
        final String nextFeeDateStr = nextFeeDate.toString();

        // Optional: disable pay button / show spinner to prevent double-clicks
        // (Assuming you have a Pay button reference)
        // payButton.setDisable(true);

        // Prepare background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1) DB write (blocking)
                final String sql = """
                        INSERT INTO fee_payments
                        (ed_no, month, amount, next_fee_date, collected_by_name, payment_mode, gateway, reference_no, due_amount)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;

                Connection conn = gradedDataLoader.databaseLoader.getConnection();
                boolean previousAutoCommit = conn.getAutoCommit();
                try {
                    conn.setAutoCommit(true);
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        int i = 0;
                        stmt.setString(++i, edNo);
                        stmt.setString(++i, mon);
                        stmt.setBigDecimal(++i, amount);
                        stmt.setString(++i, nextFeeDateStr);
                        stmt.setString(++i, collectedByName);
                        stmt.setString(++i, paymentMode);
                        stmt.setString(++i, gateway);
                        if (referenceNo == null) stmt.setNull(++i, java.sql.Types.VARCHAR);
                        else stmt.setString(++i, referenceNo);
                        stmt.setString(++i, dueAmount);
                        stmt.executeUpdate();
                    }

                    // Update last payment date too (also I/O)
                    try (PreparedStatement pst = conn.prepareStatement(
                            "UPDATE StudentData SET last_payment_date = ? WHERE ed_no = ?")) {
                        pst.setString(1, java.time.LocalDate.now().toString());
                        pst.setString(2, edNo);
                        pst.executeUpdate();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to record payment", e);
                } finally {
                    try {
                        conn.setAutoCommit(previousAutoCommit);
                    } catch (SQLException ignore) {
                    }
                }

                // 2) Telegram send (blocking network I/O)
                var student = mainController.gradedDataLoader.getStudentData().get(edNo);
                if (student != null && student.telegram_id() != null && !student.telegram_id().isEmpty()) {
                    try {
                        mainController.messageSender.sendMessage("""
                                            Fee Received
                                        
                                            Dear Parent,
                                            We have received the fee for the month %s
                                            Name : %s
                                            Amount : %s
                                            Date : %s
                                        
                                            Thank you!
                                            — Graded coaching classes
                                        """.formatted(mon, name, amount.toPlainString(), java.time.LocalDate.now()),
                                Long.parseLong(student.telegram_id()));
                    } catch (Exception ex) {
                        // Decide policy: ignore, log, or rethrow to show error
                        // Here we log and continue so DB success is not rolled back.
                        System.err.println("Telegram send failed: " + ex.getMessage());
                    }
                }
                return null;
            }
        };

        // Success → update UI safely
        task.setOnSucceeded(e -> {
            var alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Payment Confirmation");
            alert.setHeaderText("Payment done for " + edNo);
            alert.show();

            selectedMonth.getStylesheets().add(loadURL("css/paid.css").toExternalForm());
            paymentNode = splitPane.getItems().getLast();
            splitPane.getItems().set(1, mainController.gradedFxmlLoader.createView(R.payment_done_animation));
            selectedMonth = null;

            // payButton.setDisable(false);
        });

        // Failure → show error on UI
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showError(ex != null ? ex.getMessage() : "Payment failed.");
            // payButton.setDisable(false);
        });

        // Run task in a background thread
        Thread t = new Thread(task, "pay-worker");
        t.setDaemon(true);
        t.start();
    }

    // Utility to show errors
    private void showError(String header) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.show();
    }

    /**
     * Normalize a month label to the 3-letter form enforced by the CHECK constraint.
     * Returns null if it cannot be normalized.
     */
    private String normalizeMonthToAbbr(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(java.util.Locale.ENGLISH);

        // Accept common forms: "Jan", "January", "jan", etc.
        return switch (s) {
            case "jan", "january" -> "Jan";
            case "feb", "february" -> "Feb";
            case "mar", "march" -> "Mar";
            case "apr", "april" -> "Apr";
            case "may" -> "May";
            case "jun", "june" -> "Jun";
            case "jul", "july" -> "Jul";
            case "aug", "august" -> "Aug";
            case "sep", "sept", "september" -> "Sep";
            case "oct", "october" -> "Oct";
            case "nov", "november" -> "Nov";
            case "dec", "december" -> "Dec";
            default -> "";
        };
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
