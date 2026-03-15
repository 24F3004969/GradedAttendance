package org.graded_classes.graded_attendance.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.fee.FeeReceipt;
import org.graded_classes.graded_attendance.test.SnapshotUtil;

import java.io.File;
import java.io.IOException;
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
        if (!listOfMonthPaid.isEmpty()) {
            for (int i = 0; i < 12; i++) {
                if (listOfMonthPaid.contains(map.get(i))) {
                    Button button = (Button) monthsGrid.getChildren().get(i);
                    button.getStylesheets().clear();
                    button.getStylesheets().add(loadURL("css/paid.css").toExternalForm());
                } else if (!listOfMonthPaid.contains(map.get(i)) && rMap.get(map.get(i)) <= rMap.get(listOfMonthPaid.getLast()) &&
                        rMap.get(map.get(i)) >= rMap.get(listOfMonthPaid.getFirst())) {
                    Button button = (Button) monthsGrid.getChildren().get(i);
                    button.getStylesheets().clear();
                    button.getStylesheets().add(loadURL("css/unpaid.css").toExternalForm());
                }
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

    // Controller fields (create once and reuse)
    private static final java.util.concurrent.ExecutorService IO_EXEC = java.util.concurrent.Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "io-exec");
                t.setDaemon(true);
                return t;
            });

    private static final java.util.concurrent.ExecutorService NET_EXEC = java.util.concurrent.Executors.newFixedThreadPool(
            4,
            r -> {
                Thread t = new Thread(r, "net-exec");
                t.setDaemon(true);
                return t;
            });

    // Call these when your app closes to cleanly shutdown:
    public static void shutdownExecutors() {
        IO_EXEC.shutdown();
        NET_EXEC.shutdown();
    }

    private ArrayList<String> getMonthForThisStudent(String ed) {
        final String sql = """
                SELECT month
                FROM fee_payments
                WHERE ed_no = ?
                """;

        ArrayList<String> months = new ArrayList<>();
        try {
            PreparedStatement ps = mainController.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql);
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
        Parent node;

        node = (Parent) mainController.gradedFxmlLoader.createView(R.fee_receipt, new FeeReceipt(
                name,ed_no.getText(), mode.getSelectionModel().getSelectedItem(), Double.parseDouble(amount_to_pay.getText()),name_of_receiver.getText()));

        try {
            SnapshotUtil.exportFxmlNodeAsPngOffscreen(node, new File("export.png"), 6);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // ---- 1) Validate on FX thread (same as before) ----
        if (selectedMonth == null) {
            showError("Some data is missing (no month selected).");
            return;
        }

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

        final String paymentMode = java.util.Optional.ofNullable(mode.getSelectionModel().getSelectedItem()).orElse("").trim();
        if (!(paymentMode.equals("Online") || paymentMode.equals("Offline"))) {
            showError("Payment mode must be 'Online' or 'Offline'.");
            return;
        }

        final String gateway = "UPI";
        final String referenceNo = reference_no.getText().isBlank() ? null : reference_no.getText().trim();
        final String dueAmount = "0";

        // Use LocalDate for date columns (if DB column is DATE); if TEXT, stringify later.
        final java.time.LocalDate today = java.time.LocalDate.now();
        final java.time.LocalDate nextFeeDate = today.plusDays(30);

        // Optional: prevent double-pay clicks and show busy UI
        // if (payButton != null) payButton.setDisable(true);

        // ---- 2) Stage A: DB work (async on IO_EXEC) ----
        java.util.concurrent.CompletableFuture<StudentLite> dbFuture =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    // Return a minimal student object for later Telegram (id + name). Adjust to your model.
                    try {
                        java.sql.Connection conn = gradedDataLoader.databaseLoader.getConnection();
                        boolean origAuto = conn.getAutoCommit();
                        conn.setAutoCommit(false);
                        try {
                            // Insert fee_payments
                            final String insertSql = """
                                        INSERT INTO fee_payments
                                        (ed_no, month, amount, next_fee_date, collected_by_name, payment_mode, gateway, reference_no, due_amount)
                                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                                    """;
                            try {
                                java.sql.PreparedStatement ps = conn.prepareStatement(insertSql);
                                int i = 0;
                                ps.setString(++i, edNo);
                                ps.setString(++i, mon);
                                ps.setBigDecimal(++i, amount);

                                // If next_fee_date column is DATE:
                                ps.setString(++i, nextFeeDate.toString());
                                // If your column is TEXT, use:
                                // ps.setString(i, nextFeeDate.toString());

                                ps.setString(++i, collectedByName);
                                ps.setString(++i, paymentMode);
                                ps.setString(++i, gateway);
                                if (referenceNo == null) {
                                    ps.setNull(++i, java.sql.Types.VARCHAR);
                                } else {
                                    ps.setString(++i, referenceNo);
                                }
                                ps.setString(++i, dueAmount);
                                ps.executeUpdate();
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }

                            // Update StudentData.last_payment_date
                            try {
                                java.sql.PreparedStatement pst =
                                        conn.prepareStatement("UPDATE StudentData SET last_payment_date = ? WHERE ed_no = ?");
                                // If last_payment_date is DATE:
                                pst.setString(1, String.valueOf(today));
                                // If TEXT: pst.setString(1, today.toString());
                                pst.setString(2, edNo);
                                pst.executeUpdate();
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }

                            // Commit DB changes
                            conn.commit();
                            conn.setAutoCommit(origAuto);

                            // Load minimal info for telegram (id + name)
                            var student = mainController.gradedDataLoader.getStudentData().get(edNo);
                            if (student == null) {
                                return new StudentLite(null, null);
                            }
                            // Adjust accessors to your student type (record/getters)
                            String telegramId = student.telegram_id(); // or student.getTelegramId()
                            String studentName = student.name();       // or student.getName()
                            return new StudentLite(telegramId, studentName);

                        } catch (Exception ex) {
                            try {
                                conn.rollback();
                            } catch (Exception ignore) {
                            }
                            throw new RuntimeException("Failed to record payment", ex);
                        } finally {
                            try {
                                conn.setAutoCommit(true);
                            } catch (Exception ignore) {
                            }
                        }
                    } catch (java.sql.SQLException se) {
                        throw new RuntimeException("DB connection failed", se);
                    }
                }, IO_EXEC);

        // ---- 3) Stage B: Telegram (async on NET_EXEC), independent of DB commit status ----
        // We run it AFTER DB success, but we DO NOT fail the overall flow if Telegram fails.
        java.util.concurrent.CompletableFuture<Void> telegramFuture =
                dbFuture.thenAcceptAsync(studentLite -> {
                    if (studentLite == null) return;
                    if (studentLite.telegramId == null || studentLite.telegramId.isBlank()) return;

                    try {
                        String message = """
                                Fee Received
                                
                                Dear Parent,
                                We have received the fee for the month %s
                                Name : %s
                                Amount : %s
                                Date : %s
                                
                                Thank you!
                                — Graded coaching classes
                                """.formatted(mon,
                                studentLite.name == null ? "" : studentLite.name,
                                amount.toPlainString(),
                                today);

                       /* mainController.messageSender.sendMessage(
                                message,
                                Long.parseLong(studentLite.telegramId)
                        );*/
                        mainController.messageSender.sendImage(new File("export.png"), Long.parseLong(studentLite.telegramId));
                    } catch (Exception ex) {
                        // Log and continue; DB was already committed
                        System.err.println("Telegram send failed: " + ex.getMessage());
                    }
                }, NET_EXEC);

        // ---- 4) Stage C: Update UI on completion (FX thread) ----
        telegramFuture
                .handle((ok, ex) -> {
                    // Whether telegram ok or failed, we check if DB stage failed (present on dbFuture)
                    return ex;
                })
                .whenComplete((ex, ignored) -> {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            if (dbFuture.isCompletedExceptionally()) {
                                // If DB failed, extract the exception
                                dbFuture.exceptionally(dbEx -> {
                                    showError(dbEx.getMessage() != null ? dbEx.getMessage() : "Payment failed.");
                                    return null;
                                });
                            } else {
                                // Success UI path
                                var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Payment Confirmation");
                                alert.setHeaderText("Payment done for " + edNo);
                                alert.show();

                                selectedMonth.getStylesheets().add(loadURL("css/paid.css").toExternalForm());
                                paymentNode = splitPane.getItems().getLast();
                                splitPane.getItems().set(1, mainController.gradedFxmlLoader.createView(R.payment_done_animation));
                                selectedMonth = null;
                            }
                        } finally {
                            // Re-enable button / stop spinner
                            // if (payButton != null) payButton.setDisable(false);
                        }
                    });
                });
    }

    // Minimal holder for Telegram send step
    private static final class StudentLite {
        final String telegramId;
        final String name;

        StudentLite(String telegramId, String name) {
            this.telegramId = telegramId;
            this.name = name;
        }
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
