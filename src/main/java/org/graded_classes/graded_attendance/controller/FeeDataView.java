package org.graded_classes.graded_attendance.controller;

import org.graded_classes.graded_attendance.data.GradedDataLoader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeMap;

public class FeeDataView {
    GradedDataLoader gradedDataLoader;
    TreeMap<String, String[][]> feeRecords = new TreeMap<>();
    String ed;

    public FeeDataView(GradedDataLoader gradedDataLoader, String ed) {
        this.gradedDataLoader = gradedDataLoader;
        this.ed = ed;
        init();
    }

    public TreeMap<String, String[][]> getFeeRecords() {
        return feeRecords;
    }

    private void init() {
        try (Statement stmt = gradedDataLoader.databaseLoader.getStatement()) {
            ResultSet r = stmt.executeQuery("select * from fee_2026");
            while (r.next()) {
                String[][] e = new String[12][4];
                String[] months = new DateFormatSymbols(Locale.US).getShortMonths();
                for (int j = 0; j < LocalDate.now().getMonth().getValue(); j++) {
                    e[j] = getFromArray(r.getString(months[j]));
                }
                feeRecords.put(r.getString("ed_no"), e);
            }

        } catch (SQLException exception) {
            System.out.println("SQLException: " + exception.getMessage());
        }
    }

    private String[] getFromArray(String string) {
        if (string == null) {
            return new String[4];
        }
        string = string.replace("[", "").replace("]", "");
        return string.split(",");

    }
}
