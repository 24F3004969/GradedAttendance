package org.graded_classes.graded_attendance.controller.fee;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.data.FeeData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

public class FeeReport {

    @FXML
    private TableColumn<?, ?> checkboxes;

    @FXML
    private TableColumn<?, ?> doa;

    @FXML
    private TableColumn<?, ?> ed_no;

    @FXML
    private MenuButton filterMenu;

    @FXML
    private TextField filterText;

    @FXML
    private TableColumn<?, ?> grade;

    @FXML
    private TableColumn<?, ?> last_fee;

    @FXML
    private TableColumn<?, ?> last_fee1;

    @FXML
    private TableColumn<?, ?> last_fee11;

    @FXML
    private TableColumn<?, ?> last_fee2;

    @FXML
    private TableColumn<?, ?> name;
    TreeMap<String, FeeData> feeRecords = new TreeMap<>();
    @FXML
    private TableView<?> studentData;
    MainController mainController;

    public FeeReport(MainController mainController) {
        this.mainController = mainController;

        Platform.runLater(() -> {
            init();
        });
    }

    @FXML
    void onFilterMenu(ActionEvent event) {

    }

    private void init() {
        String sql = """
                    SELECT payment_id, ed_no, month, amount, paid_on, next_fee_date,
                           collected_by_name, payment_mode, gateway, reference_no, due_amount
                    FROM fee_payments
                    WHERE paid_on BETWEEN date('now', 'start of month')
                                      AND date('now', 'start of month', '+1 month', '-1 day')
                    ORDER BY paid_on , payment_id
                """;

        try (PreparedStatement ps = mainController.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    String paidOn = r.getString("paid_on");
                    String nextFeeDate = r.getString("next_fee_date");
                    String edNo = r.getString("ed_no");
                    System.out.println(paidOn + "  " + nextFeeDate);
                    FeeData fee = new FeeData(
                            r.getObject("payment_id") != null ? r.getInt("payment_id") : null,
                            edNo,
                            FeeData.MonthAbbrev.valueOf(r.getString("month")),
                            r.getDouble("amount"),
                            paidOn,
                            nextFeeDate,
                            r.getString("collected_by_name"),
                            FeeData.PaymentMode.valueOf(r.getString("payment_mode")),
                            parseGateway(r.getString("gateway")),
                            r.getString("reference_no"),
                            r.getString("due_amount")
                    );
                    feeRecords.put(r.getString("ed_no"), fee);
                }
            }
            System.out.println(feeRecords);
        } catch (SQLException exception) {
            System.out.println("SQLException: " + exception.getMessage());
        }
    }

    // Helper to handle null gateways safely
    private static FeeData.Gateway parseGateway(String g) {
        if (g == null || g.isBlank()) return null;
        return FeeData.Gateway.valueOf(g);
    }
}
