package org.graded_classes.graded_attendance.report;

import org.graded_classes.graded_attendance.data.DatabaseLoader;
import org.graded_classes.graded_attendance.data.SqlFileReader;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;

public class AttendanceReport {
    public LinkedHashMap<String, String> getStudentAttendanceReport() {
        return studentAttendanceReport;
    }

    LinkedHashMap<String, String> studentAttendanceReport = new LinkedHashMap<>();
    public DatabaseLoader databaseLoader;
    int monthIndex;
    public AttendanceReport(DatabaseLoader databaseLoader,int monthIndex) {
        this.databaseLoader = databaseLoader;
        this.monthIndex=monthIndex;
        init();
    }

    public void init() {
        try {
            var connection = databaseLoader.getStatement().getConnection();

            // CORRECTION 1: Correctly frame the exact boundaries of the selected month
            LocalDate startDate = LocalDate.of(LocalDate.now().getYear(), monthIndex, 1);
            LocalDate lastDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            System.out.println(startDate + "   " + lastDate); // Will print: 2026-01-01   2026-01-31

            var sql = new SqlFileReader("data/attendance_data_report.sql")
                    .getQuery()
                    .formatted(startDate.toString(), lastDate.toString(),
                            startDate.toString(), lastDate.toString());

            try (var pst = connection.prepareStatement(sql);
                 var r = pst.executeQuery()) {

                while (r.next()) {
                    studentAttendanceReport.put(r.getString("ed_no"),
                            r.getString("attendance_count"));
                }

            } catch (SQLException e) {
                System.err.println("SQL execution error: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println(studentAttendanceReport);
    }

}
