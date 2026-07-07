package org.graded_classes.graded_attendance.controller.fee;

import atlantafx.base.controls.SegmentedControl;
import atlantafx.base.controls.ToggleLabel;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.controller.StudentFeeLayout;
import org.graded_classes.graded_attendance.data.FeeData;
import org.graded_classes.graded_attendance.data.StudentInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FeeReport implements Initializable {
    Map<String, FeeData> sortedFeeRecords;
    @FXML
    TableColumn<FeeData, String> amount, dueData, ed_no,
            grade, mode, name, payDate, payID, referenceNo,
            dueAmount, s_no,phone_no;
    @FXML
    TableColumn<FeeData, Button> sendNotification;
    ObservableList<FeeData> items = FXCollections.observableList(new ArrayList<>());
    TreeMap<String, FeeData> duePaymentRecord, last_10_day, fine;

    @FXML
    private Label current_date;
    @FXML
    private SegmentedControl segmentControl;
    @FXML
    private TextField filterText;
    @FXML
    private Label feeCollected, total_num;
    @FXML
    private MenuButton monthList, filterMenu;
    @FXML
    private Label feeLeft;
    TreeMap<String, FeeData> feeRecords = new TreeMap<>();
    @FXML
    private TableView<FeeData> feePaidData;
    MainController mainController;
    double totalSumOfMoney, totalCollection;
    FeeRepository feeRepository = new FeeRepository();

    public FeeReport(MainController mainController) {
        this.mainController = mainController;
        paidStudentData(FeeRepository.toAbbrevFromNumber(LocalDate.now().getMonthValue()).name());

        duePaymentRecord = feeRepository.duePaymentRecord(mainController.
                gradedDataLoader.databaseLoader.getConnection(), FeeRepository.toAbbrevFromNumber(LocalDate.now().getMonthValue() - 1).name());
        last_10_day = getUpcomingFees();
    }

    @FXML
    void onFilterMenuMonth(ActionEvent event) {
        var list = monthList.getItems();
        CheckMenuItem checkMenuItem = (CheckMenuItem) event.getSource();
        for (MenuItem m : list) {
            if (((CheckMenuItem) m).isSelected()) {
                ((CheckMenuItem) m).setSelected(false);
            }
        }
        checkMenuItem.setSelected(true);
        segmentControl.getSegments().get(segmentControl.getSegments().size() - 2).
                setDisable(StudentFeeLayout.generateMonthMapInt().get(checkMenuItem.getText()) != LocalDate.now().getMonthValue() - 1);
        monthList.setText(checkMenuItem.getText());
        feeRecords.clear();
        paidStudentData(checkMenuItem.getText());
        items.clear();
        for (var keys : feeRecords.keySet()) {
            items.add(feeRecords.get(keys));
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        sortedFeeRecords = feeRecords.entrySet()
                .stream()
                .sorted((e1, e2) -> {
                    LocalDate date1 = LocalDate.parse(e1.getValue().paidOn().getValue(), formatter);
                    LocalDate date2 = LocalDate.parse(e2.getValue().paidOn().getValue(), formatter);
                    return date1.compareTo(date2);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new // Maintains the sorted order
                ));
        duePaymentRecord.clear();
        segmentControl.getSegments().getFirst().setSelected(true);
        duePaymentRecord = feeRepository.duePaymentRecord(mainController.
                gradedDataLoader.databaseLoader.getConnection(), FeeRepository.toAbbrevFromNumber
                (StudentFeeLayout.generateMonthMapInt().get(checkMenuItem.getText())).name());
        fine = feeRepository.duePaymentRecordMoreThanOneMonth(mainController.gradedDataLoader.databaseLoader.getConnection());
        double totalSumOfMoney;
        if (checkMenuItem.getText().equals(FeeRepository.toAbbrevFromNumber(LocalDate.now().getMonthValue()).name())) {
            totalSumOfMoney = mainController.gradedDataLoader.getStudentData().
                    values().stream().filter(sd ->
                            isValidMonth(sd.getDoa(), checkMenuItem.getText())).mapToDouble(sd ->
                            Double.parseDouble(sd.getFee())).sum();
        } else {
            totalSumOfMoney = getCollection("" + LocalDate.now().getYear(), checkMenuItem.getText());
        }
        double totalCollection = feeRecords.
                values().stream().mapToDouble(FeeData::amount).sum();
        System.out.println(totalCollection);
        feeCollected.setText("₹" + String.format("%,d", (long) totalCollection));
        feeLeft.setText("₹" + String.format("%,d", (int) (totalSumOfMoney - totalCollection)));
    }

    private boolean isValidMonth(String doa, String currentMonth) {
        LocalDate givenDate = LocalDate.parse(doa);
        return givenDate.getMonthValue() - 1 <= StudentFeeLayout.generateMonthMapInt().get(currentMonth);
    }

    public double getCollection(String year, String month) {
        double collection = 0.0;

        String sql = "SELECT collection FROM monthly_collection WHERE year = ? AND month = ?";

        try (PreparedStatement pstmt = mainController.gradedDataLoader.databaseLoader.
                getConnection().prepareStatement(sql)) {

            pstmt.setString(1, year);
            pstmt.setString(2, month);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                collection = rs.getDouble("collection");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return collection;
    }

    public void setCollection(String year, String month, double collection) {

        String sql = "INSERT INTO monthly_collection (year, month, collection) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT(year, month) DO UPDATE SET collection = excluded.collection";

        try (PreparedStatement pstmt = mainController.gradedDataLoader.databaseLoader.getConnection().
                prepareStatement(sql)) {

            pstmt.setString(1, year);
            pstmt.setString(2, month);
            pstmt.setDouble(3, collection);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onFilterMenuFilter(ActionEvent event) {
        var list = filterMenu.getItems();
        CheckMenuItem checkMenuItem = (CheckMenuItem) event.getSource();
        for (MenuItem m : list) {
            if (((CheckMenuItem) m).isSelected()) {
                ((CheckMenuItem) m).setSelected(false);
            }
        }
        checkMenuItem.setSelected(true);
        filterMenu.setText(checkMenuItem.getText());
    }

    private void paidStudentData(String monthName) {
        String sql = """
                    SELECT payment_id, ed_no, month, amount, paid_on, next_fee_date,
                           collected_by_name, payment_mode, gateway, reference_no, due_amount,student_name
                    FROM fee_payments
                   WHERE month='%s'
                   AND paid_on BETWEEN date('now', 'start of year') AND date('now', 'start of year', '+1 year', '-1 day')
                   ORDER BY paid_on , payment_id
                """.formatted(monthName);

        try {
            PreparedStatement ps = mainController.
                    gradedDataLoader.databaseLoader.
                    getConnection().prepareStatement(sql);
            ResultSet r = ps.executeQuery();
            int k = 0;
            String edNo;
            while (r.next()) {
                String paidOn = r.getString("paid_on");
                String nextFeeDate = r.getString("next_fee_date");
                edNo = r.getString("ed_no") == null ? "Left " + (++k) : r.getString("ed_no");
                FeeData fee = new FeeData(new StringBuilder(feeRecords.size() + 1),
                        r.getObject("payment_id") != null ?
                                r.getInt("payment_id") : null,
                        new SimpleStringProperty(edNo),
                        new SimpleStringProperty(r.getString("student_name")),
                        FeeData.MonthAbbrev.valueOf(r.getString("month")),
                        r.getDouble("amount"),
                        new SimpleStringProperty(paidOn),
                        new SimpleStringProperty(nextFeeDate),
                        new SimpleStringProperty(r.getString("collected_by_name")),
                        FeeData.PaymentMode.valueOf(r.getString("payment_mode")),
                        parseGateway(r.getString("gateway")),
                        new SimpleStringProperty(r.getString("reference_no")),
                        new SimpleStringProperty(r.getString("due_amount")),
                        new SimpleStringProperty()
                );
                feeRecords.put(edNo, fee);
            }

        } catch (SQLException exception) {
            System.out.println("SQLException: " + exception.getMessage());
        }
    }

    // Helper to handle null gateways safely
    private static FeeData.Gateway parseGateway(String g) {
        if (g == null || g.isBlank()) return null;
        return FeeData.Gateway.valueOf(g);
    }

    int index = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        feePaidData.setFixedCellSize(35);
        double totalSumOfMoney = mainController.gradedDataLoader.getStudentData().
                values().stream().mapToDouble(sd -> Double.parseDouble(sd.getFee())).sum();
        setCollection("" + LocalDate.now().getYear(), FeeRepository.toAbbrevFromNumber(LocalDate.now().getMonthValue()).name(), totalSumOfMoney);
        double totalCollection = feeRecords.
                values().stream().
                mapToDouble(FeeData::amount).sum();
        feeCollected.setText("₹" + String.format("%,d", (long) totalCollection));
        feeLeft.setText("₹" + String.format("%,d", (int) (totalSumOfMoney - totalCollection)));
        current_date.setText(LocalDate.now().getDayOfMonth() + " " +
                format(LocalDate.now().getMonth().toString()) +
                " " + LocalDate.now().getYear());
        ((CheckMenuItem) monthList.getItems().get(LocalDate.now().getMonthValue() - 1)).setSelected(true);
        monthList.setText(monthList.getItems().get(LocalDate.now().getMonthValue() - 1).getText());
        addSegmentControl();
        ed_no.setCellValueFactory(map -> map.getValue().edNo());
        name.setCellValueFactory(map -> map.getValue().name());
        amount.setCellValueFactory(map -> new SimpleStringProperty("" + map.getValue().amount()));
        dueData.setCellValueFactory(map -> map.getValue().nextFeeDate());
        mode.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().paymentMode() + ""));
        payDate.setCellValueFactory(map -> map.getValue().paidOn());
        payID.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().paymentId() + ""));
        dueAmount.setCellValueFactory(map -> map.getValue().dueAmount());
        referenceNo.setCellValueFactory(map -> map.getValue().referenceNo() == null ?
                new SimpleStringProperty("") : map.getValue().referenceNo());
        fine = feeRepository.duePaymentRecordMoreThanOneMonth(mainController.gradedDataLoader.databaseLoader.getConnection());
        s_no.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().s_no().toString()));
        phone_no.setCellValueFactory(map->map.getValue().phone_no());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        sortedFeeRecords = feeRecords.entrySet()
                .stream()
                .sorted((e1, e2) -> {
                    LocalDate date1 = LocalDate.parse(e1.getValue().paidOn().getValue(), formatter);
                    LocalDate date2 = LocalDate.parse(e2.getValue().paidOn().getValue(), formatter);
                    return date1.compareTo(date2);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
        segmentControl.getToggleGroup().selectedToggleProperty().subscribe(toggle -> {
            if (toggle instanceof ToggleLabel l) {
                if (l.getText().equals("Paid")) {
                    total_num.setText("");
                    items.clear();
                    payID.setVisible(true);
                    payDate.setVisible(true);
                    dueData.setVisible(false);
                    sendNotification.setVisible(false);
                    referenceNo.setVisible(true);
                    int dex = 1;
                    for (var keys : sortedFeeRecords.keySet()) {
                        var st = feeRecords.get(keys).s_no();
                        feeRecords.get(keys).s_no().
                                replace(0, st.length(), "" + (dex++));
                        items.add(feeRecords.get(keys));
                    }
                } else if (l.getText().equals("Unpaid for current month")) {
                    total_num.setText("");
                    items.clear();
                    payID.setVisible(false);
                    payDate.setVisible(false);
                    dueData.setVisible(true);
                    dueAmount.setVisible(false);
                    referenceNo.setVisible(false);
                    phone_no.setVisible(true);
                    sendNotification.setVisible(true);
                    sendNotification.setCellValueFactory(arg0 -> {
                        Button button = new Button("Send Notification");
                        button.setPadding(new Insets(5, 5, 5, 5));
                        button.getStyleClass().add(Styles.SUCCESS);
                        FeeData studentInfo = arg0.getValue();
                        button.setOnMouseClicked(_ -> {
                            String edNo = studentInfo.edNo().getValue();
                            String telegramID = mainController.gradedDataLoader.getStudentData().get(edNo).telegram_id();
                            mainController.messageSender.sendMessage("Your fee due date has passed.Please pay by " + LocalDate.now() + ".\nPlease pay on time as this helps us to deliver the best" +
                                    " possible coaching experience/uninterrupted service you expect.", Long.parseLong(telegramID));
                        });
                        return new SimpleObjectProperty<>(button);
                    });
                    int vex = 1;
                    for (var keys : duePaymentRecord.keySet()) {
                        var st = duePaymentRecord.get(keys).s_no();
                        duePaymentRecord.get(keys).phone_no().set(mainController.gradedDataLoader.getStudentData().get(keys).guardian_phone());
                        duePaymentRecord.get(keys).s_no().
                                replace(0, st.length(), "" + (vex++));
                        items.add(duePaymentRecord.get(keys));
                    }
                } else if (l.getText().equals("Online")) {
                    items.clear();
                    double sum = 0;
                    sendNotification.setVisible(false);
                    payDate.setVisible(true);
                    dueData.setVisible(false);
                    int tex=1;
                    for (var keys : sortedFeeRecords.keySet()) {
                        if (feeRecords.get(keys).paymentMode().equals(FeeData.PaymentMode.Online)) {
                            var st = sortedFeeRecords.get(keys).s_no();
                            sortedFeeRecords.get(keys).s_no().
                                    replace(0, st.length(), "" + (tex++));
                            items.add(feeRecords.get(keys));
                            sum += feeRecords.get(keys).amount();
                        }
                    }
                    total_num.setText("Total : ₹ " + sum);
                } else if (l.getText().equals("Offline")) {
                    items.clear();
                    double sum = 0;
                    payDate.setVisible(true);
                    sendNotification.setVisible(false);
                    dueData.setVisible(false);
                    int tex=1;
                    for (var keys : sortedFeeRecords.keySet()) {
                        if (feeRecords.get(keys).paymentMode().equals(FeeData.PaymentMode.Offline)) {
                            var st = sortedFeeRecords.get(keys).s_no();
                            sortedFeeRecords.get(keys).s_no().
                                    replace(0, st.length(), "" + (tex++));
                            items.add(feeRecords.get(keys));
                            sum += feeRecords.get(keys).amount();
                        }
                    }
                    total_num.setText("Total : ₹ " + sum);
                } else if (l.getText().equals("Due Date in 10 days")) {
                    referenceNo.setVisible(false);
                    mode.setVisible(false);
                    payDate.setVisible(false);
                    sendNotification.setVisible(false);
                    dueData.setVisible(true);
                    items.clear();
                    int tex=1;
                    for (var keys : last_10_day.keySet()) {
                        var st = last_10_day.get(keys).s_no();
                        last_10_day.get(keys).s_no().
                                replace(0, st.length(), "" + (tex++));
                        total_num.setText("");
                        items.add(last_10_day.get(keys));
                    }
                } else if (l.getText().equals("All Dues")) {
                    referenceNo.setVisible(false);
                    mode.setVisible(false);
                    payDate.setVisible(false);
                    sendNotification.setVisible(false);
                    dueData.setVisible(true);
                    items.clear();
                    int tex=1;
                    for (var keys : fine.keySet()) {
                        var st = fine.get(keys).s_no();
                        fine.get(keys).s_no().
                                replace(0, st.length(), "" + (tex++));
                        total_num.setText("");
                        items.add(fine.get(keys));
                    }
                }
            }
        });
        FilteredList<FeeData> filteredData = new FilteredList<>(items, _ -> true);
        filterText.textProperty().addListener((_, _, newValue) -> {
            filteredData.setPredicate(val -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String upperCase = newValue.toUpperCase();
                return switch (filterMenu.getText()) {
                    case "ED No." -> val.edNo().getValue().trim().contains(upperCase);
                    case "Name" -> val.name().getValue().contains(upperCase);
                    default -> false;
                };
            });
        });

        feePaidData.setItems(filteredData);
    }

    private void addSegmentControl() {
        segmentControl.getSegments().add(new ToggleLabel("Paid"));
        segmentControl.getSegments().add(new ToggleLabel("Unpaid for current month"));
        segmentControl.getSegments().add(new ToggleLabel("Online"));
        segmentControl.getSegments().add(new ToggleLabel("Offline"));
        segmentControl.getSegments().add(new ToggleLabel("Due Date in 10 days"));
        segmentControl.getSegments().add(new ToggleLabel("All Dues"));
    }

    private String format(String date) {
        return date.charAt(0) + date.substring(1).toLowerCase();
    }

    public static class FeeRepository {

        /**
         * Collects due payment records into a TreeMap keyed by ed_no.
         * <p>
         * Business rule:
         * - Include rows where DATE('now') > DATE(last_due_date)
         * - AND DATE(last_due_date) = DATE(next_fee_date)
         *
         * @param conn open SQLite JDBC connection
         * @return TreeMap<String, FeeData> keyed by ed_no
         * @throws SQLException on DB errors
         */
        public TreeMap<String, FeeData> duePaymentRecord(Connection conn, String monthName) {

            // Safer than NATURAL JOIN + SELECT *:
            //  - Picks the exact columns needed for FeeData
            //  - Ensures date comparison is on real dates, not plain text
            final String sql = """
                    SELECT
                        fp.payment_id          AS payment_id,
                        fp.ed_no               AS ed_no,
                        fp.student_name        AS student_name,
                        fp.month               AS month,           -- can be 'Jan'.. or numeric; we'll parse
                        fp.amount              AS amount,
                        fp.paid_on             AS paid_on,
                        fp.next_fee_date       AS next_fee_date,
                        fp.collected_by_name   AS collected_by_name,
                        fp.payment_mode        AS payment_mode,    -- 'Online' / 'Offline'
                        fp.gateway             AS gateway,         -- 'UPI','Card','NetBanking','Cash','Cheque'
                        fp.reference_no        AS reference_no,
                        fp.due_amount          AS due_amount
                    FROM fee_payments fp
                    JOIN DueDate dd USING (ed_no)
                    WHERE DATE('now') >= DATE(dd.last_due_date)
                      and month='%s'
                      AND DATE(dd.last_due_date) = DATE(fp.next_fee_date)
                    """.formatted(monthName);

            TreeMap<String, FeeData> map = new TreeMap<>();

            try {
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    Integer paymentId = getInteger(rs, "payment_id");
                    String edNo = rs.getString("ed_no");
                    String monthRaw = rs.getString("month");
                    double amount = rs.getDouble("amount");
                    String paidOn = rs.getString("paid_on");
                    String nextFeeDate = rs.getString("next_fee_date");
                    String collectedByName = rs.getString("collected_by_name");
                    String paymentModeRaw = rs.getString("payment_mode");
                    String gatewayRaw = rs.getString("gateway");
                    String referenceNo = rs.getString("reference_no");
                    String dueAmount = rs.getString("due_amount");

                    FeeData.MonthAbbrev month = parseMonthAbbrev(monthRaw);
                    FeeData.PaymentMode mode = parsePaymentMode(paymentModeRaw);
                    FeeData.Gateway gateway = parseGateway(gatewayRaw);

                    FeeData data = new FeeData(
                            new StringBuilder(map.size() + 1),
                            paymentId,
                            edNo,
                            rs.getString("student_name"),
                            month,
                            amount,
                            nullToEmpty(paidOn),
                            nullToEmpty(nextFeeDate),
                            nullToEmpty(collectedByName),
                            mode,
                            gateway,
                            nullToEmpty(referenceNo),
                            nullToEmpty(dueAmount),
                            new StringBuilder()
                    );

                    // Key by ed_no (change to something else if you prefer)
                    map.put(edNo, data);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return map;
        }

        public TreeMap<String, FeeData> duePaymentRecordMoreThanOneMonth(Connection conn) {

            // Safer than NATURAL JOIN + SELECT *:
            //  - Picks the exact columns needed for FeeData
            //  - Ensures date comparison is on real dates, not plain text
            final String sql = """
                    SELECT
                        fp.payment_id          AS payment_id,
                        fp.ed_no               AS ed_no,
                        fp.student_name        AS student_name,
                        fp.month               AS month,           -- can be 'Jan'.. or numeric; we'll parse
                        fp.amount              AS amount,
                        fp.paid_on             AS paid_on,
                        fp.next_fee_date       AS next_fee_date,
                        fp.collected_by_name   AS collected_by_name,
                        fp.payment_mode        AS payment_mode,    -- 'Online' / 'Offline'
                        fp.gateway             AS gateway,         -- 'UPI','Card','NetBanking','Cash','Cheque'
                        fp.reference_no        AS reference_no,
                        fp.due_amount          AS due_amount
                    FROM fee_payments fp
                             JOIN DueDate dd USING (ed_no)
                    WHERE DATE('now') >= DATE(dd.last_due_date)
                      AND DATE(dd.last_due_date) = DATE(fp.next_fee_date)
                    """;

            TreeMap<String, FeeData> map = new TreeMap<>();

            try {
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    Integer paymentId = getInteger(rs, "payment_id");
                    String edNo = rs.getString("ed_no");
                    String monthRaw = rs.getString("month");
                    double amount = rs.getDouble("amount");
                    String paidOn = rs.getString("paid_on");
                    String nextFeeDate = rs.getString("next_fee_date");
                    String collectedByName = rs.getString("collected_by_name");
                    String paymentModeRaw = rs.getString("payment_mode");
                    String gatewayRaw = rs.getString("gateway");
                    String referenceNo = rs.getString("reference_no");
                    String dueAmount = rs.getString("due_amount");

                    FeeData.MonthAbbrev month = parseMonthAbbrev(monthRaw);
                    FeeData.PaymentMode mode = parsePaymentMode(paymentModeRaw);
                    FeeData.Gateway gateway = parseGateway(gatewayRaw);

                    FeeData data = new FeeData(
                            new StringBuilder(map.size() + 1),
                            paymentId,
                            edNo,
                            rs.getString("student_name"),
                            month,
                            amount,
                            nullToEmpty(paidOn),
                            nullToEmpty(nextFeeDate),
                            nullToEmpty(collectedByName),
                            mode,
                            gateway,
                            nullToEmpty(referenceNo),
                            nullToEmpty(dueAmount),new StringBuilder()
                    );

                    // Key by ed_no (change to something else if you prefer)
                    map.put(edNo, data);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return map;
        }

        // --- Helpers -------------------------------------------------------------

        private static Integer getInteger(ResultSet rs, String col) throws SQLException {
            int v = rs.getInt(col);
            return rs.wasNull() ? null : v;
        }

        private static String nullToEmpty(String s) {
            return (s == null) ? "" : s;
        }

        private static FeeData.PaymentMode parsePaymentMode(String raw) {
            if (raw == null) return FeeData.PaymentMode.Offline; // sensible default
            String s = raw.trim().toLowerCase(Locale.ROOT);
            return switch (s) {
                case "online" -> FeeData.PaymentMode.Online;
                case "offline" -> FeeData.PaymentMode.Offline;
                default -> FeeData.PaymentMode.Offline;
            };
        }

        private static FeeData.Gateway parseGateway(String raw) {
            if (raw == null) return FeeData.Gateway.Cash;
            String s = raw.trim().toLowerCase(Locale.ROOT);
            return switch (s) {
                case "upi" -> FeeData.Gateway.UPI;
                case "card" -> FeeData.Gateway.Card;
                case "netbanking", "net_banking", "net-banking", "net banking" -> FeeData.Gateway.NetBanking;
                case "cash" -> FeeData.Gateway.Cash;
                case "cheque", "check" -> FeeData.Gateway.Cheque;
                default -> FeeData.Gateway.Cash;
            };
        }

        private static FeeData.MonthAbbrev parseMonthAbbrev(String raw) {
            if (raw == null || raw.isBlank()) return FeeData.MonthAbbrev.Jan;

            String s = raw.trim();

            // If numeric (e.g., 3 or 03 or "3")
            try {
                int m = Integer.parseInt(s);
                return toAbbrevFromNumber(m);
            } catch (NumberFormatException ignore) {
                // not numeric; try names
            }

            String lower = s.toLowerCase(Locale.ROOT);
            // Handle common full month names
            switch (lower) {
                case "january" -> {
                    return FeeData.MonthAbbrev.Jan;
                }
                case "february" -> {
                    return FeeData.MonthAbbrev.Feb;
                }
                case "march" -> {
                    return FeeData.MonthAbbrev.Mar;
                }
                case "april" -> {
                    return FeeData.MonthAbbrev.Apr;
                }
                case "may" -> {
                    return FeeData.MonthAbbrev.May;
                }
                case "june" -> {
                    return FeeData.MonthAbbrev.Jun;
                }
                case "july" -> {
                    return FeeData.MonthAbbrev.Jul;
                }
                case "august" -> {
                    return FeeData.MonthAbbrev.Aug;
                }
                case "september" -> {
                    return FeeData.MonthAbbrev.Sep;
                }
                case "october" -> {
                    return FeeData.MonthAbbrev.Oct;
                }
                case "november" -> {
                    return FeeData.MonthAbbrev.Nov;
                }
                case "december" -> {
                    return FeeData.MonthAbbrev.Dec;
                }
            }

            // 3-letter abbreviations (case-insensitive) default path:
            String abbr = s.substring(0, Math.min(3, s.length())).toLowerCase(Locale.ROOT);
            return switch (abbr) {
                case "jan" -> FeeData.MonthAbbrev.Jan;
                case "feb" -> FeeData.MonthAbbrev.Feb;
                case "mar" -> FeeData.MonthAbbrev.Mar;
                case "apr" -> FeeData.MonthAbbrev.Apr;
                case "may" -> FeeData.MonthAbbrev.May;
                case "jun" -> FeeData.MonthAbbrev.Jun;
                case "jul" -> FeeData.MonthAbbrev.Jul;
                case "aug" -> FeeData.MonthAbbrev.Aug;
                case "sep" -> FeeData.MonthAbbrev.Sep;
                case "oct" -> FeeData.MonthAbbrev.Oct;
                case "nov" -> FeeData.MonthAbbrev.Nov;
                case "dec" -> FeeData.MonthAbbrev.Dec;
                default -> FeeData.MonthAbbrev.Jan;
            };
        }

        public static FeeData.MonthAbbrev toAbbrevFromNumber(int m) {
            if (m < 1 || m > 12) return FeeData.MonthAbbrev.Jan;
            return switch (m) {
                case 1 -> FeeData.MonthAbbrev.Jan;
                case 2 -> FeeData.MonthAbbrev.Feb;
                case 3 -> FeeData.MonthAbbrev.Mar;
                case 4 -> FeeData.MonthAbbrev.Apr;
                case 5 -> FeeData.MonthAbbrev.May;
                case 6 -> FeeData.MonthAbbrev.Jun;
                case 7 -> FeeData.MonthAbbrev.Jul;
                case 8 -> FeeData.MonthAbbrev.Aug;
                case 9 -> FeeData.MonthAbbrev.Sep;
                case 10 -> FeeData.MonthAbbrev.Oct;
                case 11 -> FeeData.MonthAbbrev.Nov;
                case 12 -> FeeData.MonthAbbrev.Dec;
                default -> FeeData.MonthAbbrev.Jan;
            };
        }
    }


    @FXML
    void printNode() {
        // 1) Take a full-content snapshot of the TableView you already have:
        double scale = 2.0; // 2.0–2.5 is usually plenty; higher = larger PNG and PDF
        BufferedImage awtImg = snapshotFullTable(feePaidData, scale);

        // 2) Create a multi-page A4 PDF with margins and automatic vertical tiling
        try {
            String fileName = "fee-table-" + System.currentTimeMillis() + ".pdf";

            Path pdfPath = Paths.get(
                    System.getProperty("user.home"),
                    "",
                    fileName
            );
            createPdfFromImageTiled(awtImg, PDRectangle.A4, 36f /*0.5" margin*/, pdfPath);
            // Optionally open or show a success message
        } catch (IOException ex) {
            ex.printStackTrace();
            // handle UI error dialog as needed
        }
    }

    /**
     * Writes a PDF where the (potentially tall) image is scaled to fit the content width/height
     * and tiled vertically across as many pages as needed.
     */


    private static void createPdfFromImageTiled(BufferedImage img, PDRectangle pageSize,
                                                float margin, Path outPath) throws IOException {
        float pageW = pageSize.getWidth();
        float pageH = pageSize.getHeight();
        float availW = pageW - 2 * margin;
        float availH = pageH - 2 * margin;

        float imgW = img.getWidth();
        float imgH = img.getHeight();

        // Scale to fit the content area; change to (availW/imgW) to "fit width" strictly
        float scale = Math.min(availW / imgW, availH / imgH);
        float drawW = imgW * scale;
        float drawH = imgH * scale;

        int pages = (int) Math.ceil(drawH / availH);
        float x = margin + (availW - drawW) / 2f;

        // Encode once
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", png);
        byte[] pngBytes = png.toByteArray();

        try (PDDocument doc = new PDDocument()) {
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, pngBytes, "table");

            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage(pageSize);
                doc.addPage(page);

                float sliceTopY = margin + availH; // top of drawable content area
                float yOffset = i * availH;      // vertical advance per page

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    // Clip to content rect to avoid spillover
                    cs.addRect(margin, margin, availW, availH);
                    cs.clip();

                    float translateX = x;
                    float translateY = sliceTopY - drawH + yOffset;

                    cs.transform(new Matrix(scale, 0, 0, scale, translateX, translateY));
                    cs.drawImage(pdImage, 0, 0);
                }
            }

            doc.save(outPath.toFile());
        }
    }

    private static double getHeaderHeight(TableView<?> table) {
        // Ensure CSS/skin is created
        table.applyCss();
        table.layout();

        // Try to look up the header background node created by TableViewSkin
        Node header = table.lookup(".column-header-background");
        if (header != null) {
            return header.getBoundsInParent().getHeight();
        }

        // Fallback if lookup fails (style-dependent). Adjust as needed.
        return 28.0;
    }

    private static double computeTableFullHeight(TableView<?> table) {
        double headerH = getHeaderHeight(table);

        double rowH = table.getFixedCellSize();
        if (rowH <= 0) rowH = 24; // fallback if fixed size isn’t set

        int rows = table.getItems() == null ? 0 : table.getItems().size();

        Insets insets = table.getInsets() == null ? Insets.EMPTY : table.getInsets();
        double insetSum = insets.getTop() + insets.getBottom();

        // No scrollbar when expanded for snapshot
        return headerH + rows * rowH + insetSum;
    }

    public static BufferedImage snapshotFullTable(TableView<?> table, double scale) {
        // Save original size hints
        double oldPrefH = table.getPrefHeight();
        double oldMinH = table.getMinHeight();
        double oldMaxH = table.getMaxHeight();

        // Expand to full height
        double fullH = computeTableFullHeight(table);
        table.setMinHeight(Region.USE_PREF_SIZE);
        table.setMaxHeight(Region.USE_PREF_SIZE);
        table.setPrefHeight(fullH);

        // Force CSS/layout so virtualized rows are realized
        table.applyCss();
        table.layout();

        // Snapshot at scale
        SnapshotParameters sp = new SnapshotParameters();
        sp.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
        sp.setFill(Color.WHITE);
        WritableImage fxImg = table.snapshot(sp, null);

        // Restore original sizing
        table.setPrefHeight(oldPrefH);
        table.setMinHeight(oldMinH);
        table.setMaxHeight(oldMaxH);

        return SwingFXUtils.fromFXImage(fxImg, null);
    }

    public TreeMap<String, FeeData> getUpcomingFees() {
        TreeMap<String, FeeData> results = new TreeMap<>();

        String sql = "SELECT payment_id, DueDate.ed_no, student_name, " +
                "fee_payments.paid_on, fee_payments.due_amount, last_due_date,fee_payments.amount " +
                "FROM DueDate JOIN fee_payments ON DueDate.ed_no = fee_payments.ed_no " +
                "WHERE last_due_date BETWEEN date('now') AND date('now', '+10 days') " +
                "ORDER BY last_due_date";

        Statement stmt;
        ResultSet rs;
        try {
            stmt = mainController.
                    gradedDataLoader.databaseLoader.
                    getConnection().createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                FeeData data = new FeeData(new StringBuilder(results.size() + 1), rs.getInt("payment_id"), rs.getString("ed_no"),
                        rs.getString("student_name"), FeeData.MonthAbbrev.Jan, rs.getDouble("amount"),
                        rs.getString("paid_on"), rs.getString("last_due_date"), "Helal", FeeData.PaymentMode.Offline, FeeData.Gateway.UPI,
                        "", rs.getString("due_amount"),new StringBuilder());
                results.put(rs.getString("ed_no"), data);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return results;
    }
}

