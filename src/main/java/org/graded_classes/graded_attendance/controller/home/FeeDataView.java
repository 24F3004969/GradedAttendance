package org.graded_classes.graded_attendance.controller.home;

import org.graded_classes.graded_attendance.data.FeeData;
import org.graded_classes.graded_attendance.data.GradedDataLoader;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

public class FeeDataView {
    GradedDataLoader gradedDataLoader;
    TreeMap<String, FeeData> feeRecordsForTheCurrentMonth = new TreeMap<>();
    TreeMap<String, FeeData> duePaymentRecord = new TreeMap<>();
    String ed;

    public FeeDataView(GradedDataLoader gradedDataLoader, String ed) {
        this.gradedDataLoader = gradedDataLoader;
        this.ed = ed;
        paymentForTheCurrentMonth();
    }

    private void duePayments() {

    }

    public TreeMap<String, FeeData> getFeeRecordsForTheCurrentMonth() {
        return feeRecordsForTheCurrentMonth;
    }

    private void paymentForTheCurrentMonth() {
        String sql = """
            SELECT payment_id, ed_no, month, amount, paid_on, next_fee_date,
                   collected_by_name, payment_mode, gateway, reference_no, due_amount
            FROM fee_payments
            WHERE paid_on BETWEEN date('now', 'start of month')
                              AND date('now', 'start of month', '+1 month', '-1 day')
              AND ed_no = ?
            ORDER BY paid_on , payment_id
        """;

        try (PreparedStatement ps = gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {
            ps.setString(1, ed);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    String paidOn = r.getString("paid_on");
                    String nextFeeDate = r.getString("next_fee_date");
                    String edNo = r.getString("ed_no");
                    System.out.println(paidOn + "  " + nextFeeDate);
                    FeeData fee = new FeeData(new StringBuilder(""+(feeRecordsForTheCurrentMonth.size()+1)),
                            r.getObject("payment_id") != null ? r.getInt("payment_id") : null,
                            edNo,
                            r.getString("student_name"),
                            FeeData.MonthAbbrev.valueOf(r.getString("month")),
                            r.getDouble("amount"),
                            paidOn,
                            nextFeeDate,
                            r.getString("collected_by_name"),
                            FeeData.PaymentMode.valueOf(r.getString("payment_mode")),
                            parseGateway(r.getString("gateway")),
                            r.getString("reference_no"),
                            r.getString("due_amount"),new StringBuilder()
                    );
                    feeRecordsForTheCurrentMonth.put(r.getString("ed_no"), fee);
                }
            }
            System.out.println(feeRecordsForTheCurrentMonth);
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